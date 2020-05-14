from cassandra.cluster import Cluster
import base64

cluster = Cluster()
session = cluster.connect()
session.set_keyspace('cn1')

f = open('DJI_0006.JPG', 'rb')
img_str = base64.b64encode(f.read());

session.execute("INSERT INTO cn1.test (id, image) VALUES (%s, %s)", ("1", img_str))
