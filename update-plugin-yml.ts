#!/usr/bin/env -S deno run -RW

import { parse as parse_xml } from 'jsr:@std/xml';
import { parse as parse_yml, stringify } from "jsr:@std/yaml";

const find_node = (tree: any, name: string) => {
    if (tree.type === 'element' && tree.name?.raw === name) {
        return tree;
    }

    for (const child of tree?.children || []) {
        const found = find_node(child, name);
        if (found !== null) return found;
    }

    return null;
};

const into_paper = (dep: any) => {
    const g = dep.children.find(gc => gc.type === 'element' && gc.name.raw === 'groupId')?.children?.[0];
    if (!g) return null;
    const a = dep.children.find(gc => gc.type === 'element' && gc.name.raw === 'artifactId')?.children?.[0];
    if (!a) return null;
    const v = dep.children.find(gc => gc.type === 'element' && gc.name.raw === 'version')?.children?.[0];
    if (!v) return null;

    return [g.text, a.text, v.text];
};

const parse_dependencies = (tree: any) => 
    tree.children
        .filter(child => child.type === 'element' && child.name.raw === 'dependency')
        .map(into_paper);

const plugin_file = './src/main/resources/plugin.yml';

const pom = parse_xml(Deno.readTextFileSync('pom.xml'));
const plugin = parse_yml(Deno.readTextFileSync(plugin_file));

const dependencies = parse_dependencies(find_node(pom.root, 'dependencies'));

for (const i in plugin.libraries) {
    const existing = plugin.libraries[i];

    const [group, artifact, version] = existing.split(':');

    for (const [dg, da, dv] of dependencies) {
        if (dg === group && da === artifact && dv !== version) {
            console.log(`[${group}:${artifact}] ${version} => ${dv}`);
            plugin.libraries[i] = `${group}:${artifact}:${dv}`;
        }
    }
}

Deno.writeTextFileSync(plugin_file, stringify(plugin));
console.log('Saved.');
