from typing import List

from shapely.geometry import Polygon, Point

from election.g6.src.voter import Voter


def get_initial_triangles(voters: List[Voter], n_triangles: int, seed: int) -> List[Polygon]:
    # TODO: Derek
    pass


def get_districts_from_triangles(triangles: List[Polygon], n_districts: int, seed: int) -> List[Polygon]:
    # TODO: Adam
    pass


def get_districts(voters: List[Voter], representatives_per_district: int, seed: int) -> List[Polygon]:
    n_districts = 81
    n_triangles = n_districts * 7
    triangles = get_initial_triangles(voters, n_triangles, seed)
    districts = get_districts_from_triangles(triangles, n_districts, seed)
    return districts
