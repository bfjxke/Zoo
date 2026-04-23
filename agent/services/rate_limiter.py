import asyncio
import time

class TokenBucketRateLimiter:
    """令牌桶限流器，用于控制API调用频率"""
    
    def __init__(self, rate: int = 1, max_tokens: int = 1):
        self.rate = rate  # 每秒生成的令牌数
        self.max_tokens = max_tokens  # 最大令牌数
        self.tokens = max_tokens  # 当前令牌数
        self.last_update = time.time()  # 上次更新时间
    
    async def acquire(self, timeout: float = 30.0):
        """获取令牌，如果超时则返回False"""
        start = time.time()  # 记录开始时间
        
        while self.tokens < 1:  # 如果没有令牌可用
            if time.time() - start > timeout:  # 检查是否超时
                return False  # 超时返回False
            
            await asyncio.sleep(0.1)  # 等待100ms后重试
        
        self.tokens -= 1  # 消耗一个令牌
        return True  # 获取成功
