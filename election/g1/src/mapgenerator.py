from typing import List

from election.g1.src.voter import Voter


def get_normal(num_voters: int, mean, sigma, seed: int) -> List[Voter]:
    # @TODO Derek
    pass


def get_uniform(num_voters: int, seed: int) -> List[Voter]:
    # @TODO Derek
    pass


def get_party_preference(voters: List[Voter], num_parties: int, seed: int) -> List[Voter]:
    # @TODO Patrick
    pass


def get_coast(num_voters: int, seed: int) -> List[Voter]:
    # @TODO Patrick
    pass


def get_voters(num_voters: int, num_parties: int, seed: int) -> List[Voter]:
    # Define population distribution
    params = [{
        'type': 'coast',
        'percentage': 0.6
    }, {
        'type': 'uniform',
        'percentage': 0.2
    }, {
        'type': 'normal',
        'params': {'mean': (), 'sigma': ()},
        'percentage': 0.1
    }, {
        'type': 'normal',
        'params': {'mean': (), 'sigma': ()},
        'percentage': 0.1
    }]
    # Sample the population
    voters = []
    for index, param in enumerate(params):
        number_to_generate = round(param['percentage'] * num_voters)
        if index == len(params) - 1:
            number_to_generate = num_voters - len(voters)
        if param['type'] == 'coast':
            voters += get_coast(number_to_generate, seed=seed)
        elif param['type'] == 'uniform':
            voters += get_uniform(number_to_generate, seed=seed)
        elif param['type'] == 'normal':
            voters += get_normal(number_to_generate, param['params']['mean'], param['params']['sigma'], seed=seed)
    # Generate party preference
    voters = get_party_preference(voters, num_parties, seed)
    return voters
