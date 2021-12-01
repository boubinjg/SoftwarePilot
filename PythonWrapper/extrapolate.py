neighbors = [[1,1],[1,0],[1,-1],[0,1],[0,-1],[-1,1],[-1,0],[-1,-1]]

def init_searchzones(field_map):
    search_zones = []
    for i in range(9):
        for j in range(9):
            if(field_map[i][j] == -1):
                search_zones.append([i,j])
    return search_zones

def find_max(field_map, search_zones):
    maxZone = []
    maxPred = 0
    maxCount = 0
    for zone in search_zones:
        curPred = 0
        n_count = 0
        for n in neighbors:
            try:
                n_pred = field_map[zone[0]+n[0]][zone[1]+n[1]]
                if(n_pred != -1):
                    n_count += 1
                    curPred += n_pred
            except:
                pass
        if(n_count > maxCount):
            maxCount = n_count
            maxZone = zone
            maxPred = curPred/n_count
    return maxZone, maxPred

def extrapolate(field_map):
    search_zones = init_searchzones(field_map)
    while(len(search_zones) > 0):
        zone, pred = find_max(field_map, search_zones)
        field_map[zone[0]][zone[1]] = pred
        search_zones.remove(zone)
    return field_map
