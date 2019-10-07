from typing import Optional, List

from shapely.geometry import Point


class Voter:

    def __init__(self, location: Optional[Point] = None, preference: Optional[List[float]] = None):
        self.location = location
        self.preference = preference

    def get_file_line(self) -> str:
        line = f'{str(self.location.x)} {str(self.location.y)}'
        for p in self.preference:
            line += ' ' + str(p)
        return line
