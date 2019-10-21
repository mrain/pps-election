import shapely
from shapely.geometry import Polygon
from shapely.ops import cascaded_union

from election.g6.src import dist_analysis


class District:
    def __init__(self, representatives_per_district, n_parties):
        self.representatives_per_district = representatives_per_district
        self.n_parties = n_parties
        self.polygons = []

    def append_triangle(self, triangle):
        self.polygons.append(triangle)

    def drop_triangle(self, triangle):
        p = []
        for i, polygon in self.polygons:
            if polygon != triangle:
                p.append((i, polygon))
        self.polygons = p

    def drop_triangle_by_id(self, index):
        p = []
        for i, polygon in self.polygons:
            if i != index:
                p.append((i, polygon))
        self.polygons = p

    def get_population(self):
        population = 0
        for i, polygon in self.polygons:
            population += polygon['population']
        return population

    def get_one_polygon(self):
        f = []
        for i, p in self.polygons:
            f.append(p['polygon'])
        return cascaded_union(f)

    def get_party_distribution(self):
        party_distribution = [0] * self.n_parties
        for i, polygon in self.polygons:
            for party in polygon['party_distribution'].keys():
                party_distribution[party] += polygon['party_distribution'][party]
        return party_distribution

    def get_party_seats(self):
        party_distribution = self.get_party_distribution()
        if not self.polygons:
            return [1, 1]
        return dist_analysis.get_one_dist_seats(party_distribution, self.representatives_per_district)

    def is_invalid(self):
        s = self.get_one_polygon()
        return type(s) == shapely.geometry.MultiPolygon

    def is_population_invalid(self):
        pop = self.get_population()
        proper_pop = (333333. / (3. / self.representatives_per_district * 81.))
        if pop > 1.1 * proper_pop:
            return True
        if pop < 0.9 * proper_pop:
            return True
        return False
