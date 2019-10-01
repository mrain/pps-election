from typing import Optional, List


class Point:

    def __init__(self, x: float, y: float):
        self.x = x
        self.y = y


class Voter:

    def __init__(self, location: Optional[Point] = None, preference: Optional[List[float]] = None):
        self.location = location
        self.preference = preference
