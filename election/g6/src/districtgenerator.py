from typing import List

from shapely.geometry import Polygon

from election.g6.src.voter import Voter


def get_initial_triangles(voters: List[Voter], n_triangles: int, population: int, seed: int) -> List[Polygon]:
    # TODO: Derek
    pass


def get_districts_from_triangles(triangles: List[Polygon], n_districts: int, seed: int) -> List[Polygon]:
    # TODO: Adam
    pass


def get_districts(voters: List[Voter], representatives_per_district: int, population: int, seed: int) -> List[Polygon]:
    n_triangles = 81 * 7
    triangles = get_initial_triangles(voters, n_triangles, population, seed)
    districts = get_districts_from_triangles(triangles, 81, seed)
    return districts
