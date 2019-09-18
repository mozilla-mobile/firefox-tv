from __future__ import absolute_import, print_function, unicode_literals

from importlib import import_module


def register(graph_config):
    """
    Import all modules that are siblings of this one, triggering decorators in
    the process.
    """
    _import_modules(["job", "worker_types", "target_tasks"])


def _import_modules(modules):
    for module in modules:
        import_module(".{}".format(module), package=__name__)
