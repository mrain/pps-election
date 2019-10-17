import sys
import random
import math

import pygame
import pymunk
import pymunk.pygame_util
from pygame.color import *
from pygame.locals import *
from pymunk.vec2d import Vec2d
from networkx import Graph
from shapely.geometry import Polygon

from election.g6.src.utils import get_population_in_polygons_basic
from election.g6.src.map import Map

map_path = "maps/g7/2party.map"

voters = []
graph = None
ideal_population = 1


def add_node(space, x, y, body_type=pymunk.Body.DYNAMIC):
    b = pymunk.Body(1, 1, body_type=body_type)
    b.position = Vec2d(x, y)
    shape = pymunk.Circle(b, 6, (0, 0))
    space.add(b, shape)
    return b


def add_joint(space, b1, b2):
    rl = b1.position.get_distance(b2.position) * 0.5
    stiffness = 5000.
    damping = 100
    j = pymunk.DampedSpring(b1, b2, (0, 0), (0, 0), rl, stiffness, damping)
    j.max_bias = 1000
    space.add(j)
    return j


def add_static_segment(space, a, b):
    s = pymunk.Segment(pymunk.Body(body_type=pymunk.Body.STATIC), a, b, 3.0)
    space.add(s)
    return s


def create_graph(space, n=3):
    global graph, ideal_population, voters
    graph = Graph()
    add_static_segment(space, (500, 490 * math.sqrt(3)), (10, 10))
    add_static_segment(space, (990, 10), (10, 10))
    add_static_segment(space, (500, 490 * math.sqrt(3)), (990, 10))
    edge_length = 1000 / n
    x_diff = 1 / 2 * edge_length
    y_diff = math.sqrt(3) / 2 * edge_length
    points_by_level = [[(0, 500, 500 * math.sqrt(3))]]
    i = 1
    for level in range(n):
        new_level = []
        for index, point in enumerate(points_by_level[-1]):
            new_point = (i, point[1] - x_diff, point[2] - y_diff)
            if index == 0:
                r_min = new_point
            i += 1
            new_level.append(new_point)
        new_point = (i, point[1] + x_diff, point[2] - y_diff)
        r_max = new_point
        i += 1
        new_level.append(new_point)
        points_by_level.append(new_level)
    graph.add_nodes_from(list(range(i)))
    ideal_population = len(voters) / (i-1)
    for r in [(0, 500, 500 * math.sqrt(3)), r_min, r_max]:
        q = add_node(space, r[1], r[2], pymunk.Body.STATIC)
        graph.nodes[r[0]]['body'] = q

    for level in range(n):
        curr_level = points_by_level[level]
        next_level = points_by_level[level + 1]
        temp = [next_level[0]]
        for p1, p2 in zip(curr_level, next_level[1:]):
            temp.append(p1)
            temp.append(p2)

        for i in range(len(temp) - 2):
            p1, p2, p3 = temp[i], temp[i + 1], temp[i + 2]
            p1_i, p2_i, p3_i = p1[0], p2[0], p3[0]
            if 'body' not in graph.nodes[p1_i]:
                n = add_node(space, p1[1], p1[2])
                graph.nodes[p1_i]['body'] = n
            if 'body' not in graph.nodes[p2_i]:
                n = add_node(space, p2[1], p2[2])
                graph.nodes[p2_i]['body'] = n
            if 'body' not in graph.nodes[p3_i]:
                n = add_node(space, p3[1], p3[2])
                graph.nodes[p3_i]['body'] = n
            if not graph.has_edge(p1_i, p2_i):
                graph.add_edge(p1_i, p2_i)
                s = add_joint(space, graph.nodes[p1_i]['body'], graph.nodes[p2_i]['body'])
                graph.adj[p1_i][p2_i]['spring'] = s
            if not graph.has_edge(p2_i, p3_i):
                graph.add_edge(p2_i, p3_i)
                s = add_joint(space, graph.nodes[p2_i]['body'], graph.nodes[p3_i]['body'])
                graph.adj[p2_i][p3_i]['spring'] = s
            if not graph.has_edge(p3_i, p1_i):
                graph.add_edge(p3_i, p1_i)
                s = add_joint(space, graph.nodes[p3_i]['body'], graph.nodes[p1_i]['body'])
                graph.adj[p3_i][p1_i]['spring'] = s


def find_vertex_with_neighbours(v, w):
    vns = list(graph.neighbors(v))
    vertices = []
    for v1 in graph.neighbors(w):
        if v1 in vns:
            vertices.append(v1)
    return vertices


def recalculate_strings():
    for v, w in graph.edges():
        vertices = find_vertex_with_neighbours(v, w)
        if not vertices:
            continue
        population = 0
        p2 = graph.nodes[v]['body'].position
        p3 = graph.nodes[w]['body'].position
        for u in vertices:
            p1 = graph.nodes[u]['body'].position
            polygon = Polygon([(p1.x, p1.y), (p2.x, p2.y), (p3.x, p3.y)])
            population += get_population_in_polygons_basic(voters, [polygon])[0]
        delta_p = (len(vertices) * ideal_population - population) / (len(vertices) * ideal_population)
        distance = p2.get_distance(p3) + 0.2 * delta_p * p2.get_distance(p3)
        print(len(vertices), ideal_population, population, distance, p2.get_distance(p3))
        graph.adj[v][w]['spring'].rest_length = distance


def draw_voters(screen, voters):
    for v in voters:
        pygame.draw.circle(screen, (255, 0, 0), [int(v.location.x), int(v.location.y)], 1)


def main(map):
    global voters
    pygame.init()
    screen = pygame.display.set_mode((1000, 1000))
    clock = pygame.time.Clock()
    running = True
    voters = random.sample(map.voters, 1000)

    # Physics stuff
    space = pymunk.Space()
    draw_options = pymunk.pygame_util.DrawOptions(screen)

    create_graph(space)
    recalculate_strings()

    while running:
        for event in pygame.event.get():
            if event.type == QUIT:
                running = False
            elif event.type == KEYDOWN and event.key == K_ESCAPE:
                running = False
            elif event.type == KEYDOWN and event.key == K_p:
                pygame.image.save(screen, "damped_rotary_sprint_pointer.png")
            elif event.type == pygame.MOUSEMOTION:
                mouse_pos = pymunk.pygame_util.get_mouse_pos(screen)
                # pointer_body.position = mouse_pos
                # pointer_body.angle = (pointer_body.position - gun_body.position).angle

            elif event.type == KEYDOWN and event.key == K_q:
                # Update physics
                dt = 1.0 / 60.0
                for x in range(1):
                    space.step(dt)
            elif event.type == KEYDOWN and event.key == K_w:
                recalculate_strings()
            # elif event.type == KEYDOWN and event.key == K_a:
                # rotary_spring.damping *= .5
                # print(rotary_spring.stiffness, rotary_spring.damping)
            # elif event.type == KEYDOWN and event.key == K_s:
                # rotary_spring.damping *= 2
                # print(rotary_spring.stiffness, rotary_spring.damping)

        ### Clear screen
        screen.fill(THECOLORS["white"])

        ### Draw stuff
        space.debug_draw(draw_options)
        draw_voters(screen, voters)

        ### Flip screen
        pygame.display.flip()
        clock.tick(50)


if __name__ == '__main__':
    m = Map.from_file(map_path)
    sys.exit(main(m))
