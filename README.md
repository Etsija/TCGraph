TCGraph
=======

This is a small program to output DOT files from the MC plugin TrainCarts.  Those files can be fed into DOT tool, to print out nice graph of the pathfinding the plugin uses.

Prerequisites:

You need the "dot" tool to print out the graphs. Without it, you still get two pretty informative files
out from this program: "inputfile.txt" and "inputfile.dot".  Look into them, they are pretty self-explanatory.

Instructions:

1. Put the jarfile into the folder where your "destinations.dat" (from plugin TrainCarts) resides
2. Run this program from command line, using options if you like. "java -jar tcgraph.jar" gives you the usage
3. Program outputs "destinations.txt" and "destinations.dot".  The latter one is the one which you need from this on

5. With "dot" tool installed into your system, write on command line
> neato -tpdf destinations.dot -o destinations.pdf

6. You get a PDF file which shows the pathfinding graph in a PDF format

- - -
This work is licensed under the Creative Commons Attribution 3.0 Unported License. To view a copy of this license, visit http://creativecommons.org/licenses/by/3.0/ or send a letter to Creative Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.