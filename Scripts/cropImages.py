import sys
import re
import os
import csv
from PIL import Image

def getNum(target, content):
	tempString = re.findall(r'<' + target  + '>[0-9]*</' + target + '>', content)
	tempFinal = re.findall(r'\d+', tempString[0])
	return list(map(int, tempFinal))

def getString(target, content):
	tempString = re.findall(r'<'+target+'>(.*?)</'+target+'>', content, flags=re.S|re.M)
	return tempString

def genLine(type, location, name, point1, point2):
	return type+","+location+","+name+","+str(point1[0])+","+str(point1[1])+",,,"+str(point2[0])+","+str(point2[1])+",,"

path = sys.argv[1]
total = len(os.listdir(path))
lines = []
count = 0
for filename in os.listdir(path):
	if not filename.endswith(".xml"):
		continue
	print(filename)
	f = open(path + "/" + filename)
	content = f.read()

	width = getNum('width', content)[0]
	height = getNum('height', content)[0]

	objects = getString('object', content)
	objectList = []

	for x in objects:
		name = getString('name', x)[0]
		xmin = getNum('xmin', x)[0]
		ymin = getNum('ymin', x)[0]
		xmax = getNum('xmax', x)[0]
		ymax = getNum('ymax', x)[0]
		final = [name, xmin, ymin, xmax, ymax]
		objectList.append(final)

	for x in objectList:
		if not os.path.exists(x[0]):
			os.makedirs(x[0])
		count += 1
		img = Image.open(path + "/" + filename.replace(".xml",".jpg"))
		img = img.transpose(Image.ROTATE_270)
		img = img.crop((x[1],x[2],x[3],x[4]))
		img.save(str(x[0]) + "/" + filename.replace(".xml","-"+str(count)+".jpg"))
