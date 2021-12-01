from coapthon.client.helperclient import HelperClient
host = '192.168.1.11'
port = 5117
path = '/cr'

client = HelperClient(server=(host, port))
response = client.put(path, 'dn=DroneGimbalDriver-dc=shk')
