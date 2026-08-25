#!/usr/bin/env bash
# 农融汇 服务器端部署脚本（由 GitHub Actions SSH 调用）
set -euo pipefail

APP_DIR="/home/ubuntu/nongronghui"
BACKEND="$APP_DIR/backend"
FRONTEND_DIST="$APP_DIR/frontend/dist"
STAGING="/home/ubuntu/agri-repo"
BACKUP="/home/ubuntu/agri-backups"
JAR_NAME="platform-backend.jar"
JAVA_OPTS="-Xmx512m"
HEALTH_URL="http://127.0.0.1:8080/api/auth/login"
LOG="/var/log/agri-deploy.log"

log(){ echo "[$(date '+%F %T')] $*" | tee -a "$LOG" >&2; }
fail(){ log "❌ $*"; exit 1; }

mkdir -p "$BACKUP"
touch "$LOG" 2>/dev/null || { sudo touch "$LOG"; sudo chown ubuntu:ubuntu "$LOG"; }

[ -f "$STAGING/backend/app.jar" ] || fail "暂存区没有 jar（CI 打包失败？）"
[ -d "$STAGING/dist" ]            || fail "暂存区没有 dist"

TS=$(date +%Y%m%d-%H%M%S)
log "===== 农融汇部署开始 ====="

# 1. 备份旧版
if [ -f "$BACKEND/$JAR_NAME" ]; then
    cp "$BACKEND/$JAR_NAME" "$BACKUP/$JAR_NAME.$TS.bak"
elif ls "$BACKEND"/platform-backend-*.jar >/dev/null 2>&1; then
    cp "$(ls -t "$BACKEND"/platform-backend-*.jar | head -1)" "$BACKUP/$JAR_NAME.$TS.bak"
fi
tar -czf "$BACKUP/dist.$TS.tar.gz" -C "$(dirname "$FRONTEND_DIST")" "$(basename "$FRONTEND_DIST")" 2>/dev/null || true
log "✓ 旧版已备份"

# 2. 前端先上（静态文件，风险低）
rsync -a --delete "$STAGING/dist/" "$FRONTEND_DIST/"
log "✓ 前端已更新"

# 3. 停旧后端（温和→强杀）
if pgrep -f 'platform-backend.*\.jar' >/dev/null; then
    pkill -f 'platform-backend.*\.jar' || true
    for i in $(seq 1 15); do pgrep -f 'platform-backend.*\.jar' >/dev/null || break; sleep 1; done
    pgrep -f 'platform-backend.*\.jar' >/dev/null && { pkill -9 -f 'platform-backend.*\.jar'; sleep 2; }
    log "✓ 旧进程已停止"
fi

# 4. 换 jar（统一命名，版本号变化不影响脚本）
cp "$STAGING/backend/app.jar" "$BACKEND/$JAR_NAME"

# 5. 启动
cd "$BACKEND"
nohup java $JAVA_OPTS -jar "$JAR_NAME" > nohup.out 2>&1 &
log "→ 新进程启动中（等待 Spring Boot 就绪，最长 90 秒）…"

# 6. 健康等待（HTTP 有任何响应码 = 活了；000 = 连不上）
for i in $(seq 1 45); do
    CODE=$(curl -s -o /dev/null -w '%{http_code}' -m 2 -X POST "$HEALTH_URL" \
           -H 'Content-Type: application/json' -d '{}' || true)
    if [ -n "$CODE" ] && [ "$CODE" != "000" ]; then    
        log "✓ 后端已就绪（HTTP $CODE）"
        log "===== 部署成功 ====="
        exit 0
    fi
    sleep 2
done

# 7. 失败 → 自动回滚
log "⚠ 90 秒未就绪，自动回滚！"
OLD_JAR=$(ls -t "$BACKUP"/$JAR_NAME.*.bak 2>/dev/null | head -1 || true)
OLD_DIST=$(ls -t "$BACKUP"/dist.*.tar.gz 2>/dev/null | head -1 || true)
pkill -9 -f 'platform-backend.*\.jar' 2>/dev/null || true
sleep 2
if [ -n "$OLD_JAR" ]; then
    cp "$OLD_JAR" "$BACKEND/$JAR_NAME"
    nohup java $JAVA_OPTS -jar "$JAR_NAME" >> nohup.out 2>&1 &
    log "已回滚 jar 到 $(basename "$OLD_JAR")"
fi
[ -n "$OLD_DIST" ] && tar -xzf "$OLD_DIST" -C "$(dirname "$FRONTEND_DIST")" && log "已回滚前端"
exit 1
