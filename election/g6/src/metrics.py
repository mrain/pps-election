from typing import List

from election.g6.src import dist_analysis
from election.g6.src.district import District


def wasted_percentage_difference(given_districts: List[District], n_parties) -> List[int]:
    diffs = []
    for index, d in enumerate(given_districts):
        p = d.get_party_distribution()
        n_voters = sum(p)
        s = d.get_party_seats()
        n_seats = sum(s)
        diff = []
        for i in range(n_parties):
            diff.append(p[i]/n_voters - s[i]/n_seats)
        diffs.append(diff)
    final_diff = [0] * n_parties
    for diff in diffs:
        for i, value in enumerate(diff):
            final_diff[i] += value
    for i, value in enumerate(final_diff):
        final_diff[i] = value / len(diffs)
    return final_diff


def wasted_vote_metric(given_districts: List[District], n_parties) -> List[int]:
    party_distribution = {-1: [0] * n_parties}
    party_seats = {-1: [0] * n_parties}
    for index, d in enumerate(given_districts):
        p = d.get_party_distribution()
        s = d.get_party_seats()
        for i, pp in enumerate(p):
            party_distribution[-1][i] += pp
        party_seats[-1] = s
        party_distribution[index] = p
        party_seats[index] = s
    wasted_votes = dist_analysis.get_wasted_votes(party_distribution, party_seats)
    return wasted_votes[-1]


def get_metric(before_swap, after_swap, gerrymander_for, n_parties):
    metric = 0
    for party in range(n_parties):
        if party == gerrymander_for:
            metric += before_swap[party] - after_swap[party]
        else:
            metric += after_swap[party] - before_swap[party]
    return metric
