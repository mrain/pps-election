from typing import List, Tuple, Dict

from shapely.geometry import Polygon

from election.g6.src.voter import Voter


def get_bounding_box(polygon: Polygon) -> Tuple[float, float, float, float]:
    x_min, y_min, x_max, y_max = 1000., 1000., 0., 0.
    x_coords, y_coords = polygon.exterior.coords.xy
    for x in x_coords:
        if x < x_min:
            x_min = x
        if x > x_max:
            x_max = x
    for y in y_coords:
        if y < y_min:
            y_min = y
        if y > y_max:
            y_max = y
    return x_min, y_min, x_max, y_max


def get_voters_in_range(voters: List[Voter], p_min, p_max, p_get) -> List[Voter]:
    voters_in_range = []
    for v in voters:
        p = p_get(v)
        if p > p_max:
            break
        if p >= p_min:
            voters_in_range.append(v)
    return voters_in_range


def count_population_in_polygon(voters: List[Voter], polygon) -> int:
    count = 0
    for v in voters:
        if polygon.contains(v.location):
            count += 1
    return count


def get_population_in_polygons(voters: List[Voter], polygons: List[Polygon]) -> List[int]:
    # Sort the population according to x into two array
    voters_by_x = sorted(voters, key=lambda x: x.location.x)
    # Count population for each polygon
    population_counts = []
    for index, polygon in enumerate(polygons):
        # Find the bounding box for the polygon
        x_min, y_min, x_max, y_max = get_bounding_box(polygon)
        # Get voters in the given x
        polygon_voters_by_x = get_voters_in_range(voters_by_x, x_min, x_max, lambda x: x.location.x)
        # Sort them according to y
        polygon_voters_by_y = sorted(polygon_voters_by_x, key=lambda x: x.location.y)
        # Get voters in range of y
        polygon_voters_in_box = get_voters_in_range(polygon_voters_by_y, y_min, y_max, lambda x: x.location.y)
        # Count population in the polygon
        population = count_population_in_polygon(polygon_voters_in_box, polygon)
        population_counts.append(population)
    return population_counts


def get_population_in_polygons_basic(voters: List[Voter], polygons: List[Polygon]) -> List[int]:
    population_counts = []
    for index, polygon in enumerate(polygons):
        population = count_population_in_polygon(voters, polygon)
        population_counts.append(population)
    return population_counts


def batch(iterable, n=1):
    l = len(iterable)
    for ndx in range(0, l, n):
        yield iterable[ndx:min(ndx + n, l)]


def find_adjacent_triangle(index, triangles: List[Dict]):
    inds = []
    tr = triangles[index]
    for i, t in enumerate(triangles):
        if i == index:
            continue
        x1, y1 = t['polygon'].exterior.coords.xy
        x2, y2 = tr['polygon'].exterior.coords.xy
        n_p = 0
        for l in range(3):
            for j in range(3):
                if x1[l] == x2[j] and y1[l] == y2[j]:
                    n_p += 1
        if n_p >= 2:
            inds.append(i)
    return inds


def is_in_polygon(voter: Voter, polygon: Polygon) -> bool:
    return polygon.contains(voter.location)