#!/usr/bin/python

"""
Examples:
run-static.py ainterp -a loops scala-tools/tests/PPLTest.class "PPLTest.test1(II)I"
run-static.py taint scala-tools/tests/ Test
"""

import sys
import os
import argparse
import subprocess
import difflib

debug = True

# this driver resides at the root of the soucis source tree
soucis_basedir = os.path.dirname(__file__)

def command_ainterp(argv):
  """Abstract interpretation driver."""
  global command
  
  jarfile = os.path.join(soucis_basedir, "scala-tools/AInterp.jar")

  actions = {
    "ainterp"   : "run abstract interpreter on a single method (default)",
    "bounds"    : "run bounds computation on a single method",
    "interproc" : "run interprocedural version of the abstract interpreter",
  }

  actions_desc = '[ACTIONS]\n'
  actions_list = ''
  for action in actions.keys():
    actions_list += action + ' '
    actions_desc += "%s - %s\n" % (action, actions[action])

  argparser = argparse.ArgumentParser(
    prog="%s %s" % (os.path.basename(__file__), command),
    formatter_class=argparse.RawTextHelpFormatter,
    epilog=actions_desc,
  )

  argparser.add_argument("--action", "-a", default="ainterp", type=str,
                         help="The action to take: %s" % (actions_list))
  argparser.add_argument("--tool-args", "-t", type=str,
                         help="Pass extra args to the tool, .e.g, \"\\--debug\"")
  argparser.add_argument("--compare-file", "-c", type=argparse.FileType('r'),
                         help="Compare output to given file.")
  argparser.add_argument("classfile",
                         help="The Java class file to analyze")
  argparser.add_argument("method", # nargs="?", # TODO make method
                                   # optional once interproc is done
                         help="""The method within the class file to analyze in
  java locator format, e.g., Test(III)I is a method called Test with three
  parameters.""")

  args = argparser.parse_args(argv)

  if (debug): sys.stderr.write("action=%s, classfile=%s, method=%s\n"
                               % (args.action, args.classfile, args.method))

  tool_args = args.tool_args
  tool_args = "" if tool_args == None else tool_args
  classdir = os.path.dirname(args.classfile)
  # remove extension
  scope = "%s.*" % (os.path.basename(args.classfile).rsplit('.', 1)[0])
  method = "'%s'" % (args.method)

  # TODO: make method optional, depending on action
  command = "java -jar %s -a %s %s -D'%s' -S'%s' %s" % (jarfile,
                                                        args.action,
                                                        tool_args,
                                                        classdir,
                                                        scope, method)

  if (debug): sys.stderr.write(command + '\n')

  if (args.compare_file == None):
    redirect = None
    known_out = None
  else:
    known_out = args.compare_file.read()
    redirect = subprocess.PIPE
  
  p = subprocess.Popen(command, shell=True, stdout=redirect)

  if (args.compare_file == None):
    p.wait()
  else:
    out, _ = p.communicate()
    if known_out != out:
      sys.stdout.writelines(difflib.unified_diff(known_out.splitlines(1),
                                                 out.splitlines(1)))
      sys.exit(1)
    else:
      sys.exit(0)
  
def command_taint(argv):
  """Taint analysis driver."""
  global command

  taint_jar = os.path.join(soucis_basedir, "taint/target/taint-jar-with-dependencies.jar")
  
  argparser = argparse.ArgumentParser(
    prog="%s %s" % (os.path.basename(__file__), command),
  )

  argparser.add_argument("classpath",
                         help="The path to Java classes to analyze")
  argparser.add_argument("classname",
                         help="Name of class with a main method to analyze")

  args = argparser.parse_args(argv)

  command = "java -cp %s soucis.taint.TaintAnalysis '%s' '%s'" % (
    taint_jar, args.classpath, args.classname)

  if (debug): sys.stderr.write(command + '\n')

  subprocess.call(command, shell=True)

# handle driver commands
commands = {
  "ainterp"   : (command_ainterp, "run abstract interpretation tasks"),
  "taint"     : (command_taint,   "run taint analyses"),
}

commands_desc = '[COMMANDS]\n'
commands_list = ''
for command in commands.keys():
  commands_list += command + ' '
  commands_desc += "%s - %s\n" % (command, commands[command][1])

argparser = argparse.ArgumentParser(
  description="Driver for SOUCIS static analyses",
  formatter_class=argparse.RawTextHelpFormatter,
  epilog=commands_desc,
)

argparser.add_argument("command", help="The SOUCIS command to run")
args = argparser.parse_args(sys.argv[1:2])

command = args.command

if command not in commands.keys():
  print "invalid command"
  argparser.print_help()

if (debug): sys.stderr.write("command=%s\n" % (command))

# call the chosen command with the rest of the arguments
commands[command][0](sys.argv[2:])
