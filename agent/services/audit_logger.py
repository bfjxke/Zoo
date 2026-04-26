"""
统一日志系统 - audit_logger.py
GuardianEye-IIoT 沙箱动物园

功能：
1. 统一日志格式输出
2. 多输出目标（控制台+文件）
3. 按天自动分割
4. 违规日志单独记录
"""

import logging
from logging.handlers import TimedRotatingFileHandler
import os
from datetime import datetime
from typing import Optional


class AuditLogger:
    """
    统一日志管理器
    
    负责：
    - 控制台输出（实时查看）
    - 文件输出（持久化）
    - 违规日志分离（单独记录）
    - 按天分割（方便归档）
    """
    
    def __init__(self, log_dir: str = "logs"):
        """
        初始化审计日志器
        
        Args:
            log_dir: 日志目录，默认为logs
        """
        # 创建logs目录（如果不存在）
        os.makedirs(log_dir, exist_ok=True)
        
        # 创建logger实例
        self.logger = logging.getLogger("audit")
        self.logger.setLevel(logging.INFO)
        self.logger.handlers = []  # 清除已有的handlers，避免重复
        
        # 统一格式化器
        # 格式：[Tick #%t][%a][%f][%a] -> %r | %j[%r]
        formatter = logging.Formatter(
            "[Tick #%(tick)s][%(agent)s][%(faction)s][%(action)s] -> %(result)s | %(judge)s[%(rate)s]"
        )
        
        # 1. 控制台Handler（实时查看）
        console = logging.StreamHandler()
        console.setFormatter(formatter)
        self.logger.addHandler(console)
        
        # 2. 文件Handler（按天分割）
        # when="midnight"表示每天午夜分割
        # interval=1表示每1天分割一次
        file_handler = TimedRotatingFileHandler(
            os.path.join(log_dir, "audit.log"),
            when="midnight",  # 每天午夜分割
            interval=1,       # 每1天分割
            encoding="utf-8"  # UTF-8编码，支持中文
        )
        file_handler.setFormatter(formatter)
        self.logger.addHandler(file_handler)
        
        # 3. 违规日志Handler（单独文件）
        # 使用FileHandler而不是TimedRotatingFileHandler
        # 违规日志单独管理，方便审计
        violation_handler = logging.FileHandler(
            os.path.join(log_dir, "violation.log"),
            encoding="utf-8"
        )
        violation_handler.setLevel(logging.WARNING)  # WARNING级别及以上才记录
        violation_formatter = logging.Formatter(
            "[Tick #%(tick)s][%(agent)s][%(faction)s][%(action)s] -> %(result)s | %(judge)s[%(rate)s]"
        )
        violation_handler.setFormatter(violation_formatter)
        self.logger.addHandler(violation_handler)
        
        # 4. 系统日志Handler（单独文件）
        system_handler = logging.FileHandler(
            os.path.join(log_dir, "system.log"),
            encoding="utf-8"
        )
        system_handler.setLevel(logging.INFO)
        system_formatter = logging.Formatter(
            "[%(asctime)s] %(levelname)s: %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S"
        )
        system_handler.setFormatter(system_formatter)
        self.logger.addHandler(system_handler)
    
    def log_action(self, tick: int, agent: str, faction: str, action: str, 
                   result: str, judge_id: str = "", success_rate: Optional[float] = None):
        """
        记录Agent动作
        
        Args:
            tick: 回合数
            agent: Agent名称
            faction: 阵营
            action: 执行的动作
            result: 执行结果
            judge_id: AI判官ID（如果有）
            success_rate: 成功率（如果有）
        """
        extra = {
            "tick": tick,
            "agent": agent,
            "faction": faction,
            "action": action,
            "result": result,
            "judge": judge_id or "",
            "rate": f"{success_rate:.2f}" if success_rate is not None else ""
        }
        
        self.logger.info(
            f"{tick},{agent},{faction},{action},{result}",
            extra=extra
        )
    
    def log_violation(self, tick: int, agent: str, faction: str, action: str, reason: str):
        """
        记录违规操作
        
        违规日志会写入单独的文件（violation.log）
        
        Args:
            tick: 回合数
            agent: Agent名称
            faction: 阵营
            action: 尝试的动作
            reason: 违规原因
        """
        extra = {
            "tick": tick,
            "agent": agent,
            "faction": faction,
            "action": action,
            "result": f"违规: {reason}",
            "judge": "VIOLATION",
            "rate": ""
        }
        
        # WARNING级别会被violation_handler捕获
        self.logger.warning(
            f"违规: {tick},{agent},{faction},{action} - {reason}",
            extra=extra
        )
    
    def log_system(self, message: str):
        """
        记录系统日志
        
        系统日志记录游戏启动、结束等重要事件
        
        Args:
            message: 系统消息
        """
        self.logger.info(f"[SYSTEM] {message}")


# 全局实例（单例模式）
audit_logger: Optional[AuditLogger] = None


def get_audit_logger() -> AuditLogger:
    """
    获取全局审计日志实例
    
    使用单例模式，确保全局只有一个实例
    
    Returns:
        AuditLogger实例
    """
    global audit_logger
    if audit_logger is None:
        audit_logger = AuditLogger()
    return audit_logger


# 便捷函数
def log_action(tick: int, agent: str, faction: str, action: str, 
                result: str, judge_id: str = "", success_rate: Optional[float] = None):
    """快捷函数：记录Agent动作"""
    get_audit_logger().log_action(tick, agent, faction, action, result, judge_id, success_rate)


def log_violation(tick: int, agent: str, faction: str, action: str, reason: str):
    """快捷函数：记录违规操作"""
    get_audit_logger().log_violation(tick, agent, faction, action, reason)


def log_system(message: str):
    """快捷函数：记录系统日志"""
    get_audit_logger().log_system(message)


# 测试代码
if __name__ == "__main__":
    logger = get_audit_logger()
    
    print("=== 测试日志系统 ===")
    
    # 测试正常日志
    logger.log_action(
        tick=1,
        agent="张三",
        faction="lawful",
        action="move",
        result="center",
        judge_id="",
        success_rate=None
    )
    
    # 测试带判官的日志
    logger.log_action(
        tick=2,
        agent="李四",
        faction="aggressive",
        action="attack",
        result="成功",
        judge_id="AI_JUDGE_001",
        success_rate=0.75
    )
    
    # 测试违规日志
    logger.log_violation(
        tick=3,
        agent="王五",
        faction="neutral",
        action="move",
        reason="节点不相邻，无法移动"
    )
    
    # 测试系统日志
    logger.log_system("游戏开始初始化...")
    logger.log_system("Tick #1 开始结算")
    
    print("\n=== 测试完成 ===")
    print("日志已输出到控制台和 logs/ 目录")
    print("- logs/audit.log: 正常日志")
    print("- logs/violation.log: 违规日志")
    print("- logs/system.log: 系统日志")
