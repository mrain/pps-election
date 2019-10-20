"""
It is possible to grab one of the crossings with the mouse
"""
__version__ = "$Id:$"
__docformat__ = "reStructuredText"

import math, random

import pyglet
from election.g6.src.utils import get_population_in_polygons_basic
from election.g6.src.map import Map
import pymunk
from pymunk.vec2d import Vec2d
from pymunk.pyglet_util import DrawOptions

from shapely.geometry import Polygon, Point

from election.g6.src.trianglegenerator import naive_partition
from election.g6.src.utils import get_population_in_polygons_basic, get_population_in_polygons, save_triangles_to_file

map_path = "maps/g7/2party.map"

# load the map
m = Map.from_file(map_path)
voters = m.voters
random.seed(1234)
sampled_voters = random.sample(voters, 6666)

# ALL THE PARAMETERS
num_levels = 9
partition = naive_partition(num_levels, True)

window = pyglet.window.Window(1280, 980, vsync=False)
options = DrawOptions()

space = pymunk.Space()
space.gravity = 0, 0
space.damping = .9

force_multiplier = 100000  # multiplier for population attraction force
circle_radius = 2
circle_mass = .5
thickness = 5  # edge thickness

spring_stiffness = 3.
spring_damping_factor = 215.
spring_max_force = pymunk.inf

additional_splits = 1  # splits how many most populated triangles into three
# additional_bs = []

bs_to_triangle = {}  # maps each body points to triangles it is connected to
triangle_coord_to_population = {}  # Polygon is not hashable so I instead maps their coords to their population

# Add triangle edges
distance = thickness + circle_radius
p1 = (500, 500 * math.sqrt(3) - 2 * distance)  # top of triangle
p2 = (distance * math.sqrt(3), distance)  # left bottom
p3 = (1000 - distance * math.sqrt(3), distance)  # right bottom

body = pymunk.Body(body_type=pymunk.Body.STATIC)
l1 = pymunk.Segment(body, p1, p2, thickness)
l1.friction = 0.0
space.add(body, l1)

body = pymunk.Body(body_type=pymunk.Body.STATIC)
l2 = pymunk.Segment(body, p2, p3, thickness)
l2.friction = 0.0
space.add(body, l2)

body = pymunk.Body(body_type=pymunk.Body.STATIC)
l3 = pymunk.Segment(body, p1, p3, thickness)
l3.friction = 0.0
space.add(body, l3)

# Add points
bs = []
for level, row in enumerate(partition):
    curr = []
    for i, point in enumerate(row):
        if level == 0:
            b = pymunk.Body(body_type=pymunk.Body.STATIC)
        elif level == num_levels and (i == 0 or i == len(row) - 1):
            b = pymunk.Body(body_type=pymunk.Body.STATIC)
        else:
            b = pymunk.Body(mass=circle_mass, moment=pymunk.inf,
                            body_type=pymunk.Body.DYNAMIC)  # inf to disable rotation
        b.position = point

        s = pymunk.Circle(b, circle_radius)  # @ parameter
        space.add(b, s)
        curr.append(b)  # s
    bs.append(curr)


def add_joint(a, b):
    global spring_stiffness
    global spring_damping_factor
    global spring_max_force
    rl = a.position.get_distance(b.position) * 1.0  # @parameter
    stiffness = spring_stiffness / (9 / num_levels)
    damping = spring_damping_factor
    j = pymunk.DampedSpring(a, b, (0, 0), (0, 0), rl, stiffness, damping)
    # j.max_bias = 1000  # @parameter
    j.max_force = spring_max_force
    space.add(j)


# Connect with springs
for i in range(len(bs) - 1):
    curr_level = bs[i]
    next_level = bs[i + 1]
    for j in range(len(curr_level)):
        add_joint(curr_level[j], next_level[j])
        add_joint(curr_level[j], next_level[j + 1])
        add_joint(next_level[j], next_level[j + 1])


def draw_voters(voters):
    pos = []
    pyglet.gl.glColor3f(1, 0, 1)
    pyglet.gl.glPointSize(3)
    for voter in voters:
        pos += [voter.location.x, voter.location.y]
    pyglet.graphics.draw(len(pos) // 2, pyglet.gl.GL_POINTS, ('v2f', pos))


def get_level_coordinates(bs):
    result = []
    for level in bs:
        curr_level = []
        for body in level:
            x = float(body.position.x)
            y = float(body.position.y)
            curr_level.append((x, y))
        result.append(curr_level)
    return result


def get_triangles_from_body(bs):
    result = []
    for level in range(len(bs) - 1):
        curr_level = bs[level]
        next_level = bs[level + 1]
        temp = [next_level[0]]
        for b1, b2 in zip(curr_level, next_level[1:]):
            temp.append(b1)
            temp.append(b2)

        for i in range(len(temp) - 2):
            b1, b2, b3 = temp[i], temp[i + 1], temp[i + 2]
            result.append([b1, b2, b3])

    triangles = []
    for temp in result:
        b1, b2, b3 = temp[0], temp[1], temp[2]
        p1 = (float(b1.position.x), float(b1.position.y))
        p2 = (float(b2.position.x), float(b2.position.y))
        p3 = (float(b3.position.x), float(b3.position.y))
        polygon = Polygon([p1, p2, p3])
        triangles.append(polygon)
    return triangles


# merge the points in the simulator into shapely triangles and update the
# map that maps the points to triangles that contain the point
def get_triangles_and_update(bs):
    global bs_to_triangle
    bs_to_triangle = {body: [] for line in bs for body in line}
    result = []
    for level in range(len(bs) - 1):
        curr_level = bs[level]
        next_level = bs[level + 1]
        temp = [next_level[0]]
        for b1, b2 in zip(curr_level, next_level[1:]):
            temp.append(b1)
            temp.append(b2)

        for i in range(len(temp) - 2):
            b1, b2, b3 = temp[i], temp[i + 1], temp[i + 2]
            result.append([b1, b2, b3])

    triangles = []
    for temp in result:
        b1, b2, b3 = temp[0], temp[1], temp[2]
        p1 = (float(b1.position.x), float(b1.position.y))
        p2 = (float(b2.position.x), float(b2.position.y))
        p3 = (float(b3.position.x), float(b3.position.y))
        polygon = Polygon([p1, p2, p3])
        bs_to_triangle[b1].append(polygon)
        bs_to_triangle[b2].append(polygon)
        bs_to_triangle[b3].append(polygon)
        triangles.append(polygon)
    return triangles


def further_triangulate(triangles):
    global additional_splits
    global voters
    global sampled_voters
    if additional_splits <= 0:
        return triangles

    def split(triangle):  # TODO: better centroid
        centroid = (triangle.centroid.x, triangle.centroid.y)
        temp = list(triangle.exterior.coords)
        p1, p2, p3 = temp[0], temp[1], temp[2]
        new_triangle1 = Polygon([p1, p2, centroid])
        new_triangle2 = Polygon([p1, p3, centroid])
        new_triangle3 = Polygon([p2, p3, centroid])
        return [new_triangle1, new_triangle2, new_triangle3]

    populations = get_population_in_polygons(sampled_voters, triangles)
    temp = list(zip(triangles, populations))
    temp.sort(key=lambda x: x[1], reverse=True)  # sort by population
    triangles, _ = zip(*temp)
    new_triangles = []
    for i in range(additional_splits):
        new_triangles += split(triangles[i])
    new_triangles += triangles[additional_splits:]
    return new_triangles


# recalculate the population in each triangle
def recalculate(voters):
    global bs
    global bs_to_triangle
    global triangle_coord_to_population
    triangle_coord_to_population = {}

    triangles = get_triangles_and_update(bs)
    populations = get_population_in_polygons(voters, triangles)
    for triangle, population in zip(triangles, populations):
        coord = tuple(triangle.exterior.coords)
        triangle_coord_to_population[coord] = population


# TODO: try apply force vertically to the springs themselves, instead of points
def apply_force(n):
    global force_multiplier
    for level in bs:
        for body in level:
            if body.body_type != pymunk.Body.DYNAMIC:
                continue
            for triangle in bs_to_triangle[body]:
                bx, by = (float(body.position.x), float(body.position.y))
                centroid = triangle.centroid
                coord = tuple(triangle.exterior.coords)
                x, y = centroid.x, centroid.y
                multiplier = force_multiplier * triangle_coord_to_population[coord] / n  # @parameter
                force = (multiplier * (x - bx), multiplier * (y - by))
                body.apply_force_at_local_point(force, (0, 0))


recalculate(sampled_voters)

if additional_splits > 0:
    pass

### ALL SETUP DONE


count = 1


def update(dt):
    # Note that we dont use dt as input into step. That is because the
    # simulation will behave much better if the step size doesnt change
    # between frames.
    # space.step(dt / 10)
    global count
    count += 1
    if count % 15 == 0:  # @parameter
        recalculate(sampled_voters)
    apply_force(len(sampled_voters))
    r = 10
    for x in range(r):
        space.step(1. / 30. / r)


pyglet.clock.schedule_interval(update, 1 / 30.)

selected = None
selected_joint = None
mouse_body = pymunk.Body(body_type=pymunk.Body.KINEMATIC)


@window.event
def on_mouse_press(x, y, button, modifiers):
    mouse_body.position = x, y
    hit = space.point_query_nearest((x, y), 10, pymunk.ShapeFilter())
    if hit != None:
        global selected
        body = hit.shape.body
        rest_length = mouse_body.position.get_distance(body.position)
        stiffness = 1000
        damping = 10
        selected = pymunk.DampedSpring(mouse_body, body, (0, 0), (0, 0), rest_length, stiffness, damping)
        space.add(selected)


@window.event
def on_mouse_release(x, y, button, modifiers):
    global selected
    if selected != None:
        space.remove(selected)
        selected = None


@window.event
def on_mouse_drag(x, y, dx, dy, buttons, modifiers):
    mouse_body.position = x, y


@window.event
def on_key_press(symbol, modifiers):
    if symbol == pyglet.window.key.D:
        for voter in sampled_voters:
            print(voter.location.x, voter.location.y)
    if symbol == pyglet.window.key.F:
        res = get_triangles_and_update(bs)
        print(len(res), res)
        print(len(bs_to_triangle), bs_to_triangle)
    if symbol == pyglet.window.key.G:
        polygons = get_triangles_from_body(bs)
        res = get_population_in_polygons(sampled_voters, polygons)
        print(len(res), res)
    if symbol == pyglet.window.key.H:
        bs[4][2].apply_force_at_local_point((0, -10000), (0, 0))
        bs[4][2].apply_force_at_local_point((5000, 0), (0, 0))
    if symbol == pyglet.window.key.J:
        bs[4][2].apply_impulse_at_world_point((0, -10000), (0, 0))
    if symbol == pyglet.window.key.R:
        recalculate(sampled_voters)
    if symbol == pyglet.window.key.S:
        triangles = get_triangles_from_body(bs)
        triangles = further_triangulate(triangles)
        save_triangles_to_file(triangles)


@window.event
def on_draw():
    pyglet.gl.glClearColor(240, 240, 240, 255)
    window.clear()

    space.debug_draw(options)
    draw_voters(sampled_voters)


pyglet.app.run()
