import asyncio
import time

class TokenBucketRateLimiter:
    """令牌桶限流器，用于控制API调用频率"""
    
    def __init__(self, rate: float = 1.0, max_tokens: int = 5):
        self.rate = rate
        self.max_tokens = max_tokens
        self.tokens = float(max_tokens)
        self.last_update = time.time()
    
    async def acquire(self, timeout: float = 30.0):
        """获取令牌，如果超时则返回False"""
        start = time.time()
        
        while True:
            now = time.time()
            elapsed = now - self.last_update
            self.tokens = min(self.max_tokens, self.tokens + elapsed * self.rate)
            self.last_update = now
            
            if self.tokens >= 1.0:
                self.tokens -= 1.0
                return True
            
            if time.time() - start > timeout:
                return False
            
            await asyncio.sleep(0.05)
