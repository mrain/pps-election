from election.g6.src.map import Map
import math
from shapely.geometry import Polygon


def _find_level_points(voters, num_levels=22, special_level=1):
    voters = sorted(voters, key=lambda voter: voter.location.y, reverse=True)
    line1_x = lambda y: math.sqrt(3) / 3 * y
    line2_x = lambda y: -math.sqrt(3) / 3 * y + 1000

    def find_level(curr_voter_num, population_per_level, level):
        if curr_voter_num + population_per_level <= len(voters):
            split_voter = voters[curr_voter_num + population_per_level - 1]
            y = max(split_voter.location.y - 0.00001, 0)
        else:
            y = 0
        x_left, x_right = line1_x(y), line2_x(y)
        x_diff = (x_right - x_left) / (level - 1)
        level_points = [(x_left, y)] + [(x_left + i * x_diff, y) for i in range(1, level - 1)] + \
                       [(x_right, y)]
        return level_points

    addtional_triangles = (special_level * 2 - 1) * 2
    curr_voter_num = 0
    points_by_level = [[(500, 500 * math.sqrt(3))]]
    for level in range(2, num_levels + 1):
        if level % 2 == 0:
            population_per_triangle = math.floor(len(voters) / (num_levels ** 2 + addtional_triangles))
        else:
            population_per_triangle = math.ceil(len(voters) / (num_levels ** 2 + addtional_triangles))
        curr_level_population = population_per_triangle * (2 * (level - 2) + 1)
        if level - 1 == special_level:
            curr_level_population *= 3
        curr_level = find_level(curr_voter_num, curr_level_population, level)
        curr_voter_num += curr_level_population
        points_by_level.append(curr_level)
    # manually add all remaining voters to last level
    curr_level = find_level(curr_voter_num, len(voters) - curr_voter_num + 1, level + 1)
    points_by_level.append(curr_level)
    return points_by_level


def _points_to_triangle(points_by_level):
    result = []
    num_levels = len(points_by_level) - 1
    for level in range(num_levels):
        curr_level_triangles = []
        curr_level = points_by_level[level]
        next_level = points_by_level[level + 1]
        temp = [next_level[0]]
        for p1, p2 in zip(curr_level, next_level[1:]):
            temp.append(p1)
            temp.append(p2)

        for i in range(len(temp) - 2):
            p1, p2, p3 = temp[i], temp[i + 1], temp[i + 2]
            curr_level_triangles.append(Polygon([p1, p2, p3]))
        result.append(curr_level_triangles)
    return result


def _further_split(triangles_by_level, special_level):
    result = []
    for i in range(len(triangles_by_level)):
        curr_level = triangles_by_level[i]
        if i + 1 == special_level:
            for triangle in curr_level:
                centroid = (triangle.centroid.x, triangle.centroid.y)
                temp = list(triangle.exterior.coords)
                p1, p2, p3 = temp[0], temp[1], temp[2]
                new_triangle1 = Polygon([p1, p2, centroid])
                new_triangle2 = Polygon([p1, p3, centroid])
                new_triangle3 = Polygon([p2, p3, centroid])
                result += [new_triangle1, new_triangle2, new_triangle3]
        else:
            for triangle in curr_level:
                result.append(triangle)

    return result

# num_levels 22, 38, special_level 1, 4
def get_triangles(voters, num_levels, special_level):
    a = _find_level_points(voters, num_levels, special_level)
    b = _points_to_triangle(a)
    c = _further_split(b, special_level)
    return c

# get_triangles(22, 1)
# get_triangles(38, 4)