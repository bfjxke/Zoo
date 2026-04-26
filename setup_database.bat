@echo off
REM GuardianEye-IIoT 沙箱动物园 - 数据库初始化脚本
REM 使用前请确保 MySQL 8.0 已安装并运行

echo ========================================
echo  GuardianEye-IIoT 数据库初始化
echo ========================================
echo.

REM 检查MySQL是否运行
echo [1/3] 检查MySQL服务...
sc query MySQL80 | findstr "RUNNING"
if %errorlevel% neq 0 (
    echo [错误] MySQL服务未运行，请先启动MySQL
    pause
    exit /b 1
)
echo [OK] MySQL服务正在运行
echo.

REM 创建数据库和表
echo [2/3] 创建数据库和表结构...
mysql -u root -p4321 < "schema.sql"
if %errorlevel% neq 0 (
    echo [错误] 创建数据库失败
    pause
    exit /b 1
)
echo [OK] 数据库和表结构创建成功
echo.

REM 插入初始数据
echo [3/3] 插入初始数据...
mysql -u root -p4321 < "init_db.sql"
if %errorlevel% neq 0 (
    echo [错误] 插入初始数据失败
    pause
    exit /b 1
)
echo [OK] 初始数据插入成功
echo.

echo ========================================
echo  数据库初始化完成！
echo ========================================
echo.
echo  数据库: guardianeye_zoo
echo  用户名: root
echo  密码: 4321
echo.
pause
