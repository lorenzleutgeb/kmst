#!/bin/bash

# This script will translate input files from tests/input into Graphviz formatted syntax.
# Example usage to generate a PDF from an input file:
#	cat tests/input/0001 | ./graphvizer.sh | dot -Tpdf > /tmp/graph.pdf ; evince /tmp/graph.pdf ; rm /tmp/graph.pdf

read vertices
read edges
read k
read threshold

echo graph G {

while read edge
do
	if [ "$edge" != "" ]
	then
		echo $edge | awk '{split($0, a, " "); print a[2], " -- ", a[3], " [weight=", a[4], ", label = ", a[4], "]";}'
	fi
done

echo }
