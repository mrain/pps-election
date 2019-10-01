from typing import Optional, List


class Point:

    def __init__(self, x: float, y: float):
        self.x = x
        self.y = y


class Voter:

    def __init__(self, location: Optional[Point] = None, preference: Optional[List[float]] = None):
        self.location = location
        self.preference = preference

    def to_file_line(self) -> str:
        line = f'{str(self.location.x)} {str(self.location.y)}'
        for p in self.preference:
            line += ' ' + str(p)
        return line
