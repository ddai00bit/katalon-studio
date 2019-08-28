import sys
from pyjavaproperties import Properties
from string import Template
from build_utils import write_file, read_file

p = Properties()
p.load(open("source/com.kms.katalon/about.mappings"))

version = p['1']
print("Version", version)

branch = sys.argv[1]
print("Branch", branch)

if branch.endswith(version) is False | ("{0}.rc".format(version) in branch):
    print('Branch or version is incorrect.')
    raise ValueError('Branch or version is incorrect.')

is_qtest = "qtest" in branch
print("Is qTest", is_qtest)

is_release = ("release-" in branch) | ("-release-" in branch)
print("Is release", is_release)

is_beta = is_release & (".rc" in branch)
print("Is beta", is_beta)

with_update = is_release is True & is_qtest is False & is_beta is False
print("With update", with_update)

if is_release is True:
    tag = branch.replace('release-', '')
else:
    tag = "{0}.DEV".format(version)
print("Tag", tag)

variableTemplate = Template(
"""
#!/usr/bin/env bash

version=${version} 
isQtest=${is_qtest}
isRelease=${is_release}
isBeta=${is_beta}
withUpdate=${with_update}
tag=${tag}
""")
variable = variableTemplate.substitute(version = version, is_qtest = is_qtest, is_release = is_release, is_beta = is_beta, with_update = with_update, tag = tag)
write_file(file_path = "varable.sh", text = variable)






