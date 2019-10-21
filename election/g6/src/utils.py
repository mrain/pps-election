from typing import List, Tuple, Dict

from shapely.geometry import Polygon, MultiPolygon

from election.g6.src.voter import Voter
from election.g6.src.district import District


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


def check_if_node_is_near_part_boundary(graph, node: int) -> bool:
    part = graph.nodes[node]['part']
    for n in graph.neighbors(node):
        if graph.nodes[n]['part'] != part:
            return True
    return False


def check_if_node_is_near_part(graph, node: int, part: int) -> bool:
    for n in graph.neighbors(node):
        if graph.nodes[n]['part'] == part:
            return True
    return False

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


def get_voters_in_polygon(voters: List[Voter], polygon: Polygon) -> List[Voter]:
    vs = []
    for v in voters:
        if polygon.contains(v.location):
            vs.append(v)
    return vs


def get_all_adj_nodes_to_district(d_id, d, graph):
    #  A list of nodes and their part ids
    all_adjacent_nodes = []
    for i, poly in d.polygons:
        for node in graph.neighbors(i):
            if graph.nodes[node]['part'] != d_id:
                all_adjacent_nodes.append((graph.nodes[node]['part'], node))
    return all_adjacent_nodes


def find_adjacent_district_with_most_triangles(d_id, d, districts, graph):
    all_adjacent_nodes = get_all_adj_nodes_to_district(d_id, d, graph)
    unique_districts = set()
    for d_prime_id, n in all_adjacent_nodes:
        unique_districts.add(d_prime_id)
    dists = []
    for d_prime_id in unique_districts:
        dists.append((d_prime_id, districts[d_prime_id]))
    return sorted(dists, key=lambda x: -x[1].get_population())


def find_adjacent_district_with_least_triangles(d_id, d, districts, graph):
    pass


def check_if_removing_polygon_is_okay(district, index):
    new_d = District(district.representatives_per_district, district.n_parties)
    for i, poly in district.polygons:
        if i != index:
            new_d.append_triangle((i, poly))
    return not new_d.is_invalid()


def get_voters_in_polygons(voters: List[Voter], polygons: List[Polygon]) -> List[List[Voter]]:
    population_counts = []
    for index, polygon in enumerate(polygons):
        print('.', end='')
        population = get_voters_in_polygon(voters, polygon)
        population_counts.append(population)
    return population_counts


def get_population_in_polygons_basic(voters: List[Voter], polygons: List[Polygon]) -> List[int]:
    population_counts = []
    for index, polygon in enumerate(polygons):
        print('.', end='')
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
        x1, y1 = t.exterior.coords.xy
        x2, y2 = tr.exterior.coords.xy
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


def save_triangles_to_file(triangles, file="g6/saved_triangles.dat"):
    with open(file, 'w') as f:
        for triangle in triangles:
            coords = list(triangle.exterior.coords)
            temp = []
            for coord in coords[:3]:
                temp.append(coord[0])
                temp.append(coord[1])
            temp = [str(x) for x in temp]
            line = " ".join(temp) + '\n'
            f.write(line)
    print("File Saved")


def read_triangles_from_file(file="g6/saved_triangles.dat"):
    triangles = []
    with open(file, 'r') as f:
        for line in f:
            temp = line.split(" ")
            temp = [float(x) for x in temp]
            p1 = (temp[0], temp[1])
            p2 = (temp[2], temp[3])
            p3 = (temp[4], temp[5])
            triangle = Polygon([p1, p2, p3])
            triangles.append(triangle)
    return triangles
