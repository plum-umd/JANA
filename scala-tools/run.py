#!/usr/bin/python

import sys
import os
import argparse
import subprocess

debug = True

jar_basedir = "./ainterp"  # temporary until organization

actions = {
  "ainterp"   : ("CommandAInterp.jar",
                 "run abstract interpreter on a single method (default)"),
  "interproc" : ("CommandInterProc.jar",
                 "run interprocedural version of the abstract interpreter"),
  "loops"     : ("CommandTest.jar",
                 "run bounds computation on a single method"),
}

actions_desc = '[ACTIONS]\n'
actions_list = ''
for action in actions.keys():
  actions_list += action + ' '
  actions_desc += "%s - %s\n" % (action, actions[action][1])
    
argparser = argparse.ArgumentParser(
  description="Driver for SOUCIS static analyses",
  formatter_class=argparse.RawTextHelpFormatter,
  epilog=actions_desc,
)

argparser.add_argument("--action", "-a", default="ainterp", type=str,
                       help="The action to take: %s" % (actions_list))
argparser.add_argument("classfile",
                       help="The Java class file to analyze")
argparser.add_argument("method", # nargs="?", # TODO make method optional once interproc is done
                       help="""The method within the class file to analyze in
java locator format, e.g., Test(III)I is a method called Test with three
parameters.""")

args = argparser.parse_args()

if (debug): sys.stderr.write("action=%s, classfile=%s, method=%s\n" % (args.action, args.classfile, args.method))

jarfile = os.path.join(jar_basedir, actions[args.action][0])
classdir = os.path.dirname(args.classfile)
scope = "%s.*" % (os.path.basename(args.classfile).rsplit('.', 1)[0]) # remove extension

# TODO: make method optional, depending on action
command = "java -jar %s -D'%s' -S'%s' '%s'" % (jarfile, classdir, scope, args.method)

if (debug): sys.stderr.write(command + '\n')

subprocess.call(command, shell=True)
