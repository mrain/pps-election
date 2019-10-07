from typing import List, Optional

from shapely.geometry import Polygon, Point

from election.g6.src.voter import Voter
from election.g6.src.utils import batch
from election.g6.src.exceptions import InvalidMapFile


def get_polygon_file_line(poly):
    res = ""
    x, y = poly.exterior.coords.xy
    res += str(len(x))
    for i in range(len(x)):
        res += " " + str(x[i]) + " " + str(y[i])
    return res


class Map:

    def __init__(self, voters: Optional[List[Voter]], districts: Optional[List[Polygon]], number_of_parties: int = 3):
        self.voters = voters or []
        self.districts = districts or []
        self.number_of_parties = number_of_parties

    @staticmethod
    def from_file(filepath):
        file = open(filepath, 'r')
        voters = []
        districts = []
        lines = [line.rstrip('\n') for line in file]
        number_of_voters, number_of_parties = (int(s) for s in lines[0].split())
        for i in range(1, number_of_voters+1):
            numbers = [float(s) for s in lines[i].split()]
            point = Point(numbers[0], numbers[1])
            preferences = numbers[2:]
            voters.append(Voter(point, preferences))
        number_of_districts = int(lines[number_of_voters+1])
        for i in range(number_of_voters+2, number_of_voters+2+number_of_districts):
            numbers = lines[i].split()
            n_points = int(numbers[0])
            if n_points > 11:
                raise InvalidMapFile()
            points = [float(o) for o in numbers[1:]]
            polygon = Polygon(batch(points, 2))
            districts.append(polygon)
        file.close()
        m = Map(voters, districts, number_of_parties)
        return m

    def to_file(self, filepath):
        file = open(filepath, 'w')
        file.write(f'{str(len(self.voters))} {str(self.number_of_parties)}\n')
        for voter in self.voters:
            file.write(voter.get_file_line())
            file.write('\n')
        file.write(f'{str(len(self.districts))}\n')
        for district in self.districts:
            file.write(get_polygon_file_line(district))
            file.write('\n')
        file.close()
