import sys
import gzip
import xml.dom.minidom
import math
from geopy import distance
from geopy.point import Point
import urllib

#note: pypy is significantly faster than python on this script

commonHeader = """package boston.Bus.Map.data.prepopulated;
import java.util.ArrayList;
import java.io.IOException;

import boston.Bus.Map.transit.TransitSource;
import boston.Bus.Map.data.Directions;
import boston.Bus.Map.data.MyHashMap;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.LocationGroup;
import boston.Bus.Map.data.MultipleStopLocations;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.data.TransitDrawables;
import boston.Bus.Map.data.Direction;
import boston.Bus.Map.data.Path;
"""

individualHeader = commonHeader

header = commonHeader + """
public class {0}PrepopulatedData {1}
    private final TransitSource transitSource;
    private final Directions directions;
    private final RouteConfig[] allRoutes;
    private final MyHashMap<LocationGroup, LocationGroup> allStops;


    public {0}PrepopulatedData(TransitSource transitSource, Directions directions) throws Exception {1}
        this.transitSource = transitSource;
        this.directions = directions;
        allRoutes = makeAllRoutes();
        allStops = makeAllStops();
    {2}

    private MyHashMap<LocationGroup, LocationGroup> makeAllStops() {1}
        MyHashMap<LocationGroup, LocationGroup> ret = new MyHashMap<LocationGroup, LocationGroup>();
        for (RouteConfig route : allRoutes) {1}
            for (StopLocation stop : route.getStops()) {1}
                LocationGroup locationGroup = ret.get(stop);
                if (locationGroup != null) {1}
                    if (locationGroup instanceof MultipleStopLocations) {1}
                        ((MultipleStopLocations)locationGroup).addStop(stop);
                    {2}
                    else
                    {1}
                        MultipleStopLocations multipleStopLocations = new MultipleStopLocations();
                        multipleStopLocations.addStop((StopLocation)locationGroup);
                        multipleStopLocations.addStop(stop);
                        ret.put(multipleStopLocations, multipleStopLocations);
                    {2}
                {2}
                else
                {1}
                    ret.put(locationGroup, locationGroup);
                {2}
            {2}
        {2}
        return ret;
    {2}

    public RouteConfig[] getAllRoutes() {1}
        return allRoutes;
    {2}

"""

footer = """}
"""


def get_dom(filename):
    try:
        with gzip.open(filename, "rb") as f:
            return xml.dom.minidom.parse(f)

    except IOError:
        with open(filename, "rb") as f:
            return xml.dom.minidom.parse(f)
    
def escapeSingleQuote(s):
    if s == "'" or s == "\\":
        return "\\" + s
    else:
        return s

def escapeDoubleQuote(s):
    #TODO: implement. I don't think any stop title has a quote in it, though
    return s

def printMakeAllRoutes(routes, prefix):
    f = open(sys.argv[2] + "/boston/Bus/Map/data/prepopulated/{0}PrepopulatedData.java".format(prefix), "wb")
    f.write(header.format(prefix, "{", "}") + "\n")

    f.write("    private RouteConfig[] makeAllRoutes() throws IOException {\n")
    f.write("        return new RouteConfig[] {\n")
    for i in xrange(len(routes)):
        route = routes.values()[i]
        routeTag = route["tag"]
        f.write("            {0}PrepopulatedDataRoute{1}.makeRoute(transitSource, directions),".format(prefix, makeValid(routeTag)) + "\n")
    f.write("        };\n")
    f.write("    }\n")
    f.write(footer)
    f.close()

distanceMap = {}                        

def distanceFunc(tup1, tup2):
    # based on haversine formula for great circle distance
    _, lat1, lon1 = tup1
    _, lat2, lon2 = tup2

    #return distance.distance(Point(lat1, lon1), Point(lat2, lon2)).miles
    degreesToRadians = math.pi / 180.0

    deltaLon = (lon2 - lon1)*degreesToRadians
    deltaLat = (lat2 - lat1)*degreesToRadians

    sinResult1 = math.sin(deltaLon/2)
    sinResult2 = math.sin(deltaLat/2)
    
    

    c = 2 * math.asin(math.sqrt(sinResult1*sinResult1 + math.cos(degreesToRadians*lon1)*math.cos(degreesToRadians*lon2)*sinResult2*sinResult2))
    earthRadiusInKilo = 6371.2
    kiloPerMile = 1.609344
    dist = earthRadiusInKilo * c
    ret = dist/kiloPerMile
    return ret


def humanize(locKey):
    return str(locKey).replace("-", "_").replace(".", "_").replace(",", "_").replace(" ", "_").replace("(", "_").replace(")", "_")

def makeValid(s):
    return s.replace("-", "_").replace("/", "_").replace(" ", "_")

def printEachMakeRoute(routes, prefix):
    for i in xrange(len(routes.values())):
        route = routes.values()[i]
        routeTag = route["tag"]
        f = open(sys.argv[2] + "/boston/Bus/Map/data/prepopulated/{0}PrepopulatedDataRoute{1}.java".format(prefix, makeValid(routeTag)), "wb")
        f.write(individualHeader)
        f.write("public class {0}PrepopulatedDataRoute{1} {2}\n".format(prefix, makeValid(routeTag), "{"))
        f.write("    public static RouteConfig makeRoute(TransitSource transitSource, Directions directions) throws IOException {1}".format(i, "{") + "\n")
        f.write("        TransitDrawables drawables = transitSource.getDrawables();\n")
        f.write("        RouteConfig route = new RouteConfig(\"{0}\", \"{1}\", 0x{2}, 0x{3}, transitSource);".format(routeTag, route["title"], route["color"], route["oppositeColor"]) + "\n")

        for stop in route["stops"].values():
            stopTag = stop["tag"]
            f.write("        StopLocation stop{0} = new StopLocation({1}f, {2}f, drawables, \"{4}\", \"{3}\");".format(makeValid(stopTag), stop["lat"], stop["lon"], stop["title"], stopTag) + "\n")
            f.write("        route.addStop(\"{0}\", stop{1});".format(stopTag, makeValid(stopTag)) + "\n")

        for direction in route["directions"].values():
            f.write("            directions.add(\"{0}\", new Direction(\"{1}\", \"{2}\", \"{3}\"));".format(direction["tag"], direction["name"], direction["title"], routeTag) + "\n")
            for dirChild in direction["stops"]:
                dirStopTag = dirChild["tag"]
                #TODO: in order for direction
                #f.write("            route.addStop(\"{0}\", stop{1});".format(dirStopTag, dirStopTag) + "\n")
                #f.write("            stop{0}.addRoute(\"{1}\");\n".format(dirStopTag, routeTag))
            
        f.write("            ArrayList<Path> paths = new ArrayList<Path>();\n")
        for pointCount in xrange((len(route["path"]) / 500) + 1):
            f.write("            paths.add(makePath{0}());\n".format(pointCount))
        f.write("            route.setPaths(paths.toArray(new Path[0]));\n")
        f.write("        return route;\n")
        f.write("    }\n")

        for pointCount in xrange((len(route["path"]) / 500) + 1):
            f.write("    private static Path makePath{0}() {1}\n".format(pointCount, "{"))
            f.write("        return new Path(new float[] {\n")
            for point in route["path"][500*pointCount:500*(pointCount+1)]:
                f.write("            {0}f, {1}f,\n".format(point[0], point[1]))
            f.write("        });\n")
            f.write("    }\n")

        f.write("}\n")
        f.close()


def nextbusToRoutes(routesXml):
    routes = {}
    for routeXml in routesXml.getElementsByTagName("route"):
        stops = {}
        directions = {}
        path = []
        routeTag = routeXml.getAttribute("tag")
        route = {"tag":routeTag, "title": routeXml.getAttribute("title"), "color": routeXml.getAttribute("color"), "oppositeColor": routeXml.getAttribute("oppositeColor"), "stops": stops, "directions": directions, "path" : path}
        routes[routeTag] = route
        for routeChildXml in routeXml.childNodes:
            if routeChildXml.nodeName == "stop":
                stopTag = routeChildXml.getAttribute("tag")
                stops[stopTag] = {"tag": stopTag, "title": routeChildXml.getAttribute("title"), "lat": routeChildXml.getAttribute("lat"), "lon": routeChildXml.getAttribute("lon")}
            elif routeChildXml.nodeName == "direction":
                dirTag = routeChildXml.getAttribute("tag")
                directionStops = []
                direction = {"tag" : dirTag, "title": routeChildXml.getAttribute("title"), "name": routeChildXml.getAttribute("name"), "stops": directionStops}
                directions[dirTag] = direction
                for directionChildXml in routeChildXml.childNodes:
                    if directionChildXml.nodeName == "stop":
                        directionStops.append({"tag": directionChildXml.getAttribute("tag")})
            elif routeChildXml.nodeName == "point":
                point = (routeChildXml.getAttribute("lat"), routeChildXml.getAttribute("lon"))
                path.append(point)

    return routes

def runPrepopulated(routes, prefix):
    printMakeAllRoutes(routes, prefix)
    printEachMakeRoute(routes, prefix)


def csvToMap(header, line):
    ret = {}
    for i in xrange(len(header)):
        h = header[i]
        item = line[i]
        ret[h] = item

    return ret

def getColor(routeTag):
    m = {"Red" : "ff0000", "Orange" : "f88017", "Blue": "0000ff"}
    return m[routeTag]

def commuterRailRoutes():
    dataFromMBTA = """route_long_name,direction_id,stop_sequence,stop_id,stop_lat,stop_lon,Branch
Fairmount Line,0,1,South Station,42.352614,-71.055364,Trunk
Fairmount Line,0,2,Uphams Corner,42.318670,-71.069072,Trunk
Fairmount Line,0,3,Morton Street,42.280994,-71.085475,Trunk
Fairmount Line,0,4,Fairmount,42.253638,-71.119270,Trunk
Fairmount Line,0,5,Readville,42.237750,-71.132376,Trunk
Fairmount Line,1,1,Readville,42.237750,-71.132376,Trunk
Fairmount Line,1,2,Fairmount,42.253638,-71.119270,Trunk
Fairmount Line,1,3,Morton Street,42.280994,-71.085475,Trunk
Fairmount Line,1,4,Uphams Corner,42.318670,-71.069072,Trunk
Fairmount Line,1,5,South Station,42.352614,-71.055364,Trunk
Fitchburg/South Acton Line,0,1,North Station,42.365551,-71.061251,Trunk
Fitchburg/South Acton Line,0,2,Porter Square,42.388353,-71.119159,Trunk
Fitchburg/South Acton Line,0,3,Belmont,42.398420,-71.174499,Trunk
Fitchburg/South Acton Line,0,4,Waverley,42.387489,-71.189864,Trunk
Fitchburg/South Acton Line,0,5,Waltham,42.374424,-71.236595,Trunk
Fitchburg/South Acton Line,0,6,Brandeis/ Roberts,42.361728,-71.260854,Trunk
Fitchburg/South Acton Line,0,7,Kendal Green,42.378970,-71.282411,Trunk
Fitchburg/South Acton Line,0,8,Hastings,42.385755,-71.289203,Trunk
Fitchburg/South Acton Line,0,9,Silver Hill,42.395625,-71.302357,Trunk
Fitchburg/South Acton Line,0,10,Lincoln,42.414229,-71.325344,Trunk
Fitchburg/South Acton Line,0,11,Concord,42.457147,-71.358051,Trunk
Fitchburg/South Acton Line,0,12,West Concord,42.456372,-71.392371,Trunk
Fitchburg/South Acton Line,0,13,South Acton,42.461575,-71.455322,Trunk
Fitchburg/South Acton Line,0,14,Littleton / Rte 495,42.519236,-71.502643,Trunk
Fitchburg/South Acton Line,0,15,Ayer,42.560047,-71.590117,Trunk
Fitchburg/South Acton Line,0,16,Shirley,42.544726,-71.648363,Trunk
Fitchburg/South Acton Line,0,17,North Leominster,42.540577,-71.739402,Trunk
Fitchburg/South Acton Line,0,18,Fitchburg,42.581739,-71.792750,Trunk
Fitchburg/South Acton Line,1,1,Fitchburg,42.581739,-71.792750,Trunk
Fitchburg/South Acton Line,1,2,North Leominster,42.540577,-71.739402,Trunk
Fitchburg/South Acton Line,1,3,Shirley,42.544726,-71.648363,Trunk
Fitchburg/South Acton Line,1,4,Ayer,42.560047,-71.590117,Trunk
Fitchburg/South Acton Line,1,5,Littleton / Rte 495,42.519236,-71.502643,Trunk
Fitchburg/South Acton Line,1,6,South Acton,42.461575,-71.455322,Trunk
Fitchburg/South Acton Line,1,7,West Concord,42.456372,-71.392371,Trunk
Fitchburg/South Acton Line,1,8,Concord,42.457147,-71.358051,Trunk
Fitchburg/South Acton Line,1,9,Lincoln,42.414229,-71.325344,Trunk
Fitchburg/South Acton Line,1,10,Silver Hill,42.395625,-71.302357,Trunk
Fitchburg/South Acton Line,1,11,Hastings,42.385755,-71.289203,Trunk
Fitchburg/South Acton Line,1,12,Kendal Green,42.378970,-71.282411,Trunk
Fitchburg/South Acton Line,1,13,Brandeis/ Roberts,42.361728,-71.260854,Trunk
Fitchburg/South Acton Line,1,14,Waltham,42.374424,-71.236595,Trunk
Fitchburg/South Acton Line,1,15,Waverley,42.387489,-71.189864,Trunk
Fitchburg/South Acton Line,1,16,Belmont,42.398420,-71.174499,Trunk
Fitchburg/South Acton Line,1,17,Porter Square,42.388353,-71.119159,Trunk
Fitchburg/South Acton Line,1,18,North Station,42.365551,-71.061251,Trunk
Framingham/Worcester Line,0,1,South Station,42.352614,-71.055364,Trunk
Framingham/Worcester Line,0,2,Back Bay,42.347158,-71.075769,Trunk
Framingham/Worcester Line,0,3,Yawkey,42.346796,-71.098937,Trunk
Framingham/Worcester Line,0,4,Newtonville,42.351603,-71.207338,Trunk
Framingham/Worcester Line,0,5,West Newton,42.348599,-71.229010,Trunk
Framingham/Worcester Line,0,6,Auburndale,42.346087,-71.246658,Trunk
Framingham/Worcester Line,0,7,Wellesley Farms,42.323608,-71.272288,Trunk
Framingham/Worcester Line,0,8,Wellesley Hills,42.310027,-71.276769,Trunk
Framingham/Worcester Line,0,9,Wellesley Square,42.296427,-71.294311,Trunk
Framingham/Worcester Line,0,10,Natick,42.285239,-71.347641,Trunk
Framingham/Worcester Line,0,11,West Natick,42.281855,-71.390548,Trunk
Framingham/Worcester Line,0,12,Framingham,42.276719,-71.416792,Trunk
Framingham/Worcester Line,0,13,Ashland,42.261694,-71.478813,Trunk
Framingham/Worcester Line,0,14,Southborough,42.267518,-71.523621,Trunk
Framingham/Worcester Line,0,15,Westborough,42.269184,-71.652005,Trunk
Framingham/Worcester Line,0,16,Grafton,42.246291,-71.684614,Trunk
Framingham/Worcester Line,0,17,Worcester / Union Station,42.261796,-71.793881,Trunk
Framingham/Worcester Line,1,1,Worcester / Union Station,42.261796,-71.793881,Trunk
Framingham/Worcester Line,1,2,Grafton,42.246291,-71.684614,Trunk
Framingham/Worcester Line,1,3,Westborough,42.269184,-71.652005,Trunk
Framingham/Worcester Line,1,4,Southborough,42.267518,-71.523621,Trunk
Framingham/Worcester Line,1,5,Ashland,42.261694,-71.478813,Trunk
Framingham/Worcester Line,1,6,Framingham,42.276719,-71.416792,Trunk
Framingham/Worcester Line,1,7,West Natick,42.281855,-71.390548,Trunk
Framingham/Worcester Line,1,8,Natick,42.285239,-71.347641,Trunk
Framingham/Worcester Line,1,9,Wellesley Square,42.296427,-71.294311,Trunk
Framingham/Worcester Line,1,10,Wellesley Hills,42.310027,-71.276769,Trunk
Framingham/Worcester Line,1,11,Wellesley Farms,42.323608,-71.272288,Trunk
Framingham/Worcester Line,1,12,Auburndale,42.346087,-71.246658,Trunk
Framingham/Worcester Line,1,13,West Newton,42.348599,-71.229010,Trunk
Framingham/Worcester Line,1,14,Newtonville,42.351603,-71.207338,Trunk
Framingham/Worcester Line,1,15,Yawkey,42.346796,-71.098937,Trunk
Framingham/Worcester Line,1,16,Back Bay,42.347158,-71.075769,Trunk
Framingham/Worcester Line,1,17,South Station,42.352614,-71.055364,Trunk
Franklin Line,0,1,South Station,42.352614,-71.055364,Trunk
Franklin Line,0,2,Back Bay,42.347158,-71.075769,Trunk
Franklin Line,0,3,Ruggles,42.335545,-71.090524,Trunk
Franklin Line,0,4,Hyde Park,42.255121,-71.125022,Trunk
Franklin Line,0,5,Readville,42.237750,-71.132376,Trunk
Franklin Line,0,6,Endicott,42.232881,-71.160413,Trunk
Franklin Line,0,7,Dedham Corp Center,42.225896,-71.173806,Trunk
Franklin Line,0,8,Islington,42.220714,-71.183406,Trunk
Franklin Line,0,9,Norwood Depot,42.195668,-71.196784,Trunk
Franklin Line,0,10,Norwood Central,42.190776,-71.199748,Trunk
Franklin Line,0,11,Windsor Gardens,42.172192,-71.220704,Trunk
Franklin Line,0,12,Plimptonville,42.159123,-71.236125,Trunk
Franklin Line,0,13,Walpole,42.144192,-71.259016,Trunk
Franklin Line,0,14,Norfolk,42.120694,-71.325217,Trunk
Franklin Line,0,15,Franklin,42.083591,-71.396735,Trunk
Franklin Line,0,16,Forge Park / 495,42.090704,-71.430342,Trunk
Franklin Line,1,1,Forge Park / 495,42.090704,-71.430342,Trunk
Franklin Line,1,2,Franklin,42.083591,-71.396735,Trunk
Franklin Line,1,3,Norfolk,42.120694,-71.325217,Trunk
Franklin Line,1,4,Walpole,42.144192,-71.259016,Trunk
Franklin Line,1,5,Plimptonville,42.159123,-71.236125,Trunk
Franklin Line,1,6,Windsor Gardens,42.172192,-71.220704,Trunk
Franklin Line,1,7,Norwood Central,42.190776,-71.199748,Trunk
Franklin Line,1,8,Norwood Depot,42.195668,-71.196784,Trunk
Franklin Line,1,9,Islington,42.220714,-71.183406,Trunk
Franklin Line,1,10,Dedham Corp Center,42.225896,-71.173806,Trunk
Franklin Line,1,11,Endicott,42.232881,-71.160413,Trunk
Franklin Line,1,12,Readville,42.237750,-71.132376,Trunk
Franklin Line,1,13,Hyde Park,42.255121,-71.125022,Primary
Franklin Line,1,13,Fairmount,42.253638,-71.119270,Secondary
Franklin Line,1,14,Ruggles,42.335545,-71.090524,Primary
Franklin Line,1,14,Morton Street,42.280994,-71.085475,Secondary
Franklin Line,1,15,Uphams Corner,42.318670,-71.069072,Secondary
Franklin Line,1,15,Back Bay,42.347158,-71.075769,Primary
Franklin Line,1,16,South Station,42.352614,-71.055364,Trunk
Greenbush Line,0,1,South Station,42.352614,-71.055364,Trunk
Greenbush Line,0,2,JFK/UMASS,42.321123,-71.052555,Trunk
Greenbush Line,0,3,Quincy Center,42.250862,-71.004843,Trunk
Greenbush Line,0,4,Weymouth Landing/ East Braintree,42.220800,-70.968200,Trunk
Greenbush Line,0,5,East Weymouth,42.219100,-70.921400,Trunk
Greenbush Line,0,6,West Hingham,42.236700,-70.903100,Trunk
Greenbush Line,0,7,Nantasket Junction,42.245200,-70.869800,Trunk
Greenbush Line,0,8,Cohasset,42.242400,-70.837000,Trunk
Greenbush Line,0,9,North Scituate,42.219700,-70.787700,Trunk
Greenbush Line,0,10,Greenbush,42.178100,-70.746200,Trunk
Greenbush Line,1,1,Greenbush,42.178100,-70.746200,Trunk
Greenbush Line,1,2,North Scituate,42.219700,-70.787700,Trunk
Greenbush Line,1,3,Cohasset,42.242400,-70.837000,Trunk
Greenbush Line,1,4,Nantasket Junction,42.245200,-70.869800,Trunk
Greenbush Line,1,5,West Hingham,42.236700,-70.903100,Trunk
Greenbush Line,1,6,East Weymouth,42.219100,-70.921400,Trunk
Greenbush Line,1,7,Weymouth Landing/ East Braintree,42.220800,-70.968200,Trunk
Greenbush Line,1,8,Quincy Center,42.250862,-71.004843,Trunk
Greenbush Line,1,9,JFK/UMASS,42.321123,-71.052555,Trunk
Greenbush Line,1,10,South Station,42.352614,-71.055364,Trunk
Haverhill Line,0,1,North Station,42.365551,-71.061251,Trunk
Haverhill Line,0,2,West Medford,42.421184,-71.132468,Secondary
Haverhill Line,0,2,Malden Center,42.426407,-71.074227,Primary
Haverhill Line,0,3,Wyoming Hill,42.452097,-71.069518,Primary
Haverhill Line,0,3,Wedgemere,42.445284,-71.140909,Secondary
Haverhill Line,0,4,Melrose Cedar Park,42.459128,-71.069448,Primary
Haverhill Line,0,4,Winchester Center,42.452650,-71.137041,Secondary
Haverhill Line,0,5,Anderson/ Woburn,42.518082,-71.138650,Secondary
Haverhill Line,0,5,Melrose Highlands,42.468793,-71.068270,Primary
Haverhill Line,0,6,Greenwood,42.483473,-71.067233,Primary
Haverhill Line,0,6,Wilmington,42.546368,-71.173569,Secondary
Haverhill Line,0,7,Wakefield,42.501811,-71.075000,Primary
Haverhill Line,0,8,Reading,42.521480,-71.107440,Primary
Haverhill Line,0,11,North Wilmington,42.568462,-71.159724,Primary
Haverhill Line,0,12,Ballardvale,42.626449,-71.159653,Trunk
Haverhill Line,0,13,Andover,42.657798,-71.144513,Trunk
Haverhill Line,0,14,Lawrence,42.700094,-71.159797,Trunk
Haverhill Line,0,15,Bradford,42.768899,-71.085998,Trunk
Haverhill Line,0,16,Haverhill,42.772684,-71.085962,Trunk
Haverhill Line,1,1,Haverhill,42.772684,-71.085962,Trunk
Haverhill Line,1,2,Bradford,42.768899,-71.085998,Trunk
Haverhill Line,1,3,Lawrence,42.700094,-71.159797,Trunk
Haverhill Line,1,4,Andover,42.657798,-71.144513,Trunk
Haverhill Line,1,5,Ballardvale,42.626449,-71.159653,Trunk
Haverhill Line,1,6,North Wilmington,42.568462,-71.159724,Trunk
Haverhill Line,1,7,Wilmington,42.546368,-71.173569,Secondary
Haverhill Line,1,8,Anderson/ Woburn,42.518082,-71.138650,Secondary
Haverhill Line,1,9,Winchester Center,42.452650,-71.137041,Secondary
Haverhill Line,1,9,Reading,42.521480,-71.107440,Primary
Haverhill Line,1,10,Wedgemere,42.445284,-71.140909,Secondary
Haverhill Line,1,10,Wakefield,42.501811,-71.075000,Primary
Haverhill Line,1,11,Greenwood,42.483473,-71.067233,Primary
Haverhill Line,1,11,West Medford,42.421184,-71.132468,Secondary
Haverhill Line,1,12,Melrose Highlands,42.468793,-71.068270,Primary
Haverhill Line,1,13,Melrose Cedar Park,42.459128,-71.069448,Primary
Haverhill Line,1,14,Wyoming Hill,42.452097,-71.069518,Primary
Haverhill Line,1,15,Malden Center,42.426407,-71.074227,Primary
Haverhill Line,1,16,North Station,42.365551,-71.061251,Trunk
Kingston/Plymouth Line,0,1,South Station,42.352614,-71.055364,Trunk
Kingston/Plymouth Line,0,2,JFK/UMASS,42.321123,-71.052555,Trunk
Kingston/Plymouth Line,0,3,Quincy Center,42.250862,-71.004843,Trunk
Kingston/Plymouth Line,0,4,Braintree,42.208550,-71.000850,Trunk
Kingston/Plymouth Line,0,11,South Weymouth,42.153747,-70.952490,Trunk
Kingston/Plymouth Line,0,12,Abington,42.108034,-70.935296,Trunk
Kingston/Plymouth Line,0,13,Whitman,42.083563,-70.923204,Trunk
Kingston/Plymouth Line,0,14,Hanson,42.043262,-70.881553,Trunk
Kingston/Plymouth Line,0,15,Halifax,42.012867,-70.820832,Trunk
Kingston/Plymouth Line,0,17,Plymouth,41.981184,-70.692514,Secondary
Kingston/Plymouth Line,0,17,Kingston,41.978548,-70.720315,Primary
Kingston/Plymouth Line,1,2,Plymouth,41.981184,-70.692514,Secondary
Kingston/Plymouth Line,1,2,Kingston,41.978548,-70.720315,Primary
Kingston/Plymouth Line,1,3,Halifax,42.012867,-70.820832,Trunk
Kingston/Plymouth Line,1,4,Hanson,42.043262,-70.881553,Trunk
Kingston/Plymouth Line,1,5,Whitman,42.083563,-70.923204,Trunk
Kingston/Plymouth Line,1,6,Abington,42.108034,-70.935296,Trunk
Kingston/Plymouth Line,1,7,South Weymouth,42.153747,-70.952490,Trunk
Kingston/Plymouth Line,1,14,Braintree,42.208550,-71.000850,Trunk
Kingston/Plymouth Line,1,15,Quincy Center,42.250862,-71.004843,Trunk
Kingston/Plymouth Line,1,16,JFK/UMASS,42.321123,-71.052555,Trunk
Kingston/Plymouth Line,1,17,South Station,42.352614,-71.055364,Trunk
Lowell Line,0,1,North Station,42.365551,-71.061251,Trunk
Lowell Line,0,2,West Medford,42.421184,-71.132468,Trunk
Lowell Line,0,3,Wedgemere,42.445284,-71.140909,Trunk
Lowell Line,0,4,Winchester Center,42.452650,-71.137041,Trunk
Lowell Line,0,5,Mishawum,42.503595,-71.137511,Trunk
Lowell Line,0,6,Anderson/ Woburn,42.518082,-71.138650,Trunk
Lowell Line,0,7,Wilmington,42.546368,-71.173569,Trunk
Lowell Line,0,9,North Billerica,42.592881,-71.280869,Trunk
Lowell Line,0,10,Lowell,42.638402,-71.314916,Trunk
Lowell Line,1,1,Lowell,42.638402,-71.314916,Trunk
Lowell Line,1,2,North Billerica,42.592881,-71.280869,Trunk
Lowell Line,1,4,Wilmington,42.546368,-71.173569,Trunk
Lowell Line,1,5,Anderson/ Woburn,42.518082,-71.138650,Trunk
Lowell Line,1,6,Mishawum,42.503595,-71.137511,Trunk
Lowell Line,1,7,Winchester Center,42.452650,-71.137041,Trunk
Lowell Line,1,8,Wedgemere,42.445284,-71.140909,Trunk
Lowell Line,1,9,West Medford,42.421184,-71.132468,Trunk
Lowell Line,1,10,North Station,42.365551,-71.061251,Trunk
Middleborough/Lakeville Line,0,1,South Station,42.352614,-71.055364,Trunk
Middleborough/Lakeville Line,0,2,JFK/UMASS,42.321123,-71.052555,Trunk
Middleborough/Lakeville Line,0,3,Quincy Center,42.250862,-71.004843,Trunk
Middleborough/Lakeville Line,0,4,Braintree,42.208550,-71.000850,Trunk
Middleborough/Lakeville Line,0,5,Holbrook/ Randolph,42.155314,-71.027518,Trunk
Middleborough/Lakeville Line,0,6,Montello,42.106047,-71.021078,Trunk
Middleborough/Lakeville Line,0,7,Brockton,42.085720,-71.016860,Trunk
Middleborough/Lakeville Line,0,8,Campello,42.060038,-71.012460,Trunk
Middleborough/Lakeville Line,0,9,Bridgewater,41.986355,-70.966625,Trunk
Middleborough/Lakeville Line,0,10,Middleboro/ Lakeville,41.878210,-70.918444,Trunk
Middleborough/Lakeville Line,1,8,Middleboro/ Lakeville,41.878210,-70.918444,Trunk
Middleborough/Lakeville Line,1,9,Bridgewater,41.986355,-70.966625,Trunk
Middleborough/Lakeville Line,1,10,Campello,42.060038,-71.012460,Trunk
Middleborough/Lakeville Line,1,11,Brockton,42.085720,-71.016860,Trunk
Middleborough/Lakeville Line,1,12,Montello,42.106047,-71.021078,Trunk
Middleborough/Lakeville Line,1,13,Holbrook/ Randolph,42.155314,-71.027518,Trunk
Middleborough/Lakeville Line,1,14,Braintree,42.208550,-71.000850,Trunk
Middleborough/Lakeville Line,1,15,Quincy Center,42.250862,-71.004843,Trunk
Middleborough/Lakeville Line,1,16,JFK/UMASS,42.321123,-71.052555,Trunk
Middleborough/Lakeville Line,1,17,South Station,42.352614,-71.055364,Trunk
Needham Line,0,1,South Station,42.352614,-71.055364,Trunk
Needham Line,0,2,Back Bay,42.347158,-71.075769,Trunk
Needham Line,0,3,Ruggles,42.335545,-71.090524,Trunk
Needham Line,0,4,Forest Hills,42.300023,-71.113377,Trunk
Needham Line,0,5,Roslindale Village,42.287206,-71.129610,Trunk
Needham Line,0,6,Bellevue,42.287138,-71.146060,Trunk
Needham Line,0,7,Highland,42.284869,-71.154700,Trunk
Needham Line,0,8,West Roxbury,42.281600,-71.159932,Trunk
Needham Line,0,9,Hersey,42.275842,-71.214853,Trunk
Needham Line,0,10,Needham Junction,42.273327,-71.238007,Trunk
Needham Line,0,11,Needham Center,42.280274,-71.238089,Trunk
Needham Line,0,12,Needham Heights,42.293139,-71.235087,Trunk
Needham Line,1,1,Needham Heights,42.293139,-71.235087,Trunk
Needham Line,1,2,Needham Center,42.280274,-71.238089,Trunk
Needham Line,1,3,Needham Junction,42.273327,-71.238007,Trunk
Needham Line,1,4,Hersey,42.275842,-71.214853,Trunk
Needham Line,1,5,West Roxbury,42.281600,-71.159932,Trunk
Needham Line,1,6,Highland,42.284869,-71.154700,Trunk
Needham Line,1,7,Bellevue,42.287138,-71.146060,Trunk
Needham Line,1,8,Roslindale Village,42.287206,-71.129610,Trunk
Needham Line,1,9,Forest Hills,42.300023,-71.113377,Trunk
Needham Line,1,10,Ruggles,42.335545,-71.090524,Trunk
Needham Line,1,11,Back Bay,42.347158,-71.075769,Trunk
Needham Line,1,12,South Station,42.352614,-71.055364,Trunk
Newburyport/Rockport Line,0,1,North Station,42.365551,-71.061251,Trunk
Newburyport/Rockport Line,0,2,Chelsea,42.395661,-71.034826,Trunk
Newburyport/Rockport Line,0,3,River Works,42.453804,-70.975698,Trunk
Newburyport/Rockport Line,0,4,Lynn,42.462293,-70.947794,Trunk
Newburyport/Rockport Line,0,5,Swampscott,42.473739,-70.922036,Trunk
Newburyport/Rockport Line,0,6,Salem,42.523927,-70.898903,Trunk
Newburyport/Rockport Line,0,7,Beverly,42.546907,-70.885168,Trunk
Newburyport/Rockport Line,0,8,North Beverly,42.582471,-70.884501,Trunk
Newburyport/Rockport Line,0,9,Hamilton/ Wenham,42.610756,-70.874005,Trunk
Newburyport/Rockport Line,0,10,Ipswich,42.678355,-70.840024,Trunk
Newburyport/Rockport Line,0,11,Rowley,42.725351,-70.859436,Trunk
Newburyport/Rockport Line,0,12,Newburyport,42.800292,-70.880262,Trunk
Newburyport/Rockport Line,0,13,Montserrat,42.561483,-70.870035,Trunk
Newburyport/Rockport Line,0,14,Prides Crossing,42.559513,-70.824813,Trunk
Newburyport/Rockport Line,0,15,Beverly Farms,42.561403,-70.812745,Trunk
Newburyport/Rockport Line,0,16,Manchester,42.573570,-70.770473,Trunk
Newburyport/Rockport Line,0,17,West Gloucester,42.610928,-70.706456,Trunk
Newburyport/Rockport Line,0,18,Gloucester,42.616069,-70.668767,Trunk
Newburyport/Rockport Line,0,19,Rockport,42.656173,-70.625616,Trunk
Newburyport/Rockport Line,1,1,Rockport,42.656173,-70.625616,Trunk
Newburyport/Rockport Line,1,2,Gloucester,42.616069,-70.668767,Trunk
Newburyport/Rockport Line,1,3,West Gloucester,42.610928,-70.706456,Trunk
Newburyport/Rockport Line,1,4,Manchester,42.573570,-70.770473,Trunk
Newburyport/Rockport Line,1,5,Beverly Farms,42.561403,-70.812745,Trunk
Newburyport/Rockport Line,1,6,Prides Crossing,42.559513,-70.824813,Trunk
Newburyport/Rockport Line,1,7,Montserrat,42.561483,-70.870035,Trunk
Newburyport/Rockport Line,1,8,Newburyport,42.800292,-70.880262,Trunk
Newburyport/Rockport Line,1,9,Rowley,42.725351,-70.859436,Trunk
Newburyport/Rockport Line,1,10,Ipswich,42.678355,-70.840024,Trunk
Newburyport/Rockport Line,1,11,Hamilton/ Wenham,42.610756,-70.874005,Trunk
Newburyport/Rockport Line,1,12,North Beverly,42.582471,-70.884501,Trunk
Newburyport/Rockport Line,1,13,Beverly,42.546907,-70.885168,Trunk
Newburyport/Rockport Line,1,14,Salem,42.523927,-70.898903,Trunk
Newburyport/Rockport Line,1,15,Swampscott,42.473739,-70.922036,Trunk
Newburyport/Rockport Line,1,16,Lynn,42.462293,-70.947794,Trunk
Newburyport/Rockport Line,1,17,River Works,42.453804,-70.975698,Trunk
Newburyport/Rockport Line,1,18,Chelsea,42.395661,-71.034826,Trunk
Newburyport/Rockport Line,1,19,North Station,42.365551,-71.061251,Trunk
Providence/Stoughton Line,0,1,South Station,42.352614,-71.055364,Trunk
Providence/Stoughton Line,0,2,Back Bay,42.347158,-71.075769,Trunk
Providence/Stoughton Line,0,3,Ruggles,42.335545,-71.090524,Trunk
Providence/Stoughton Line,0,4,Hyde Park,42.255121,-71.125022,Trunk
Providence/Stoughton Line,0,5,Route 128,42.209884,-71.147100,Trunk
Providence/Stoughton Line,0,6,Canton Junction,42.163423,-71.153374,Trunk
Providence/Stoughton Line,0,7,Canton Center,42.156769,-71.145530,Secondary
Providence/Stoughton Line,0,8,Stoughton,42.123818,-71.103090,Secondary
Providence/Stoughton Line,0,9,Sharon,42.124804,-71.183213,Primary
Providence/Stoughton Line,0,10,Mansfield,42.032734,-71.219318,Primary
Providence/Stoughton Line,0,11,Attleboro,41.942097,-71.284897,Primary
Providence/Stoughton Line,0,12,South Attleboro,41.897943,-71.354621,Primary
Providence/Stoughton Line,0,13,Providence,41.829641,-71.413332,Primary
Providence/Stoughton Line,0,14,TF Green Airport,41.726599,-71.442453,Primary
Providence/Stoughton Line,1,0,TF Green Airport,41.726599,-71.442453,Primary
Providence/Stoughton Line,1,1,Providence,41.829641,-71.413332,Primary
Providence/Stoughton Line,1,2,South Attleboro,41.897943,-71.354621,Primary
Providence/Stoughton Line,1,3,Attleboro,41.942097,-71.284897,Primary
Providence/Stoughton Line,1,4,Mansfield,42.032734,-71.219318,Primary
Providence/Stoughton Line,1,5,Sharon,42.124804,-71.183213,Primary
Providence/Stoughton Line,1,6,Stoughton,42.123818,-71.103090,Secondary
Providence/Stoughton Line,1,7,Canton Center,42.156769,-71.145530,Secondary
Providence/Stoughton Line,1,8,Canton Junction,42.163423,-71.153374,Trunk
Providence/Stoughton Line,1,9,Route 128,42.209884,-71.147100,Trunk
Providence/Stoughton Line,1,10,Hyde Park,42.255121,-71.125022,Trunk
Providence/Stoughton Line,1,11,Ruggles,42.335545,-71.090524,Trunk
Providence/Stoughton Line,1,12,Back Bay,42.347158,-71.075769,Trunk
Providence/Stoughton Line,1,13,South Station,42.352614,-71.055364,Trunk"""
    routeNames = ["Greenbush",
                  "Kingston/Plymouth",
                  "Middleborough/Lakeville",
                  "Fairmount",
                  "Providence/Stoughton",
                  "Franklin",
                  "Needham",
                  "Framingham/Worcester",
                  "Fitchburg/South Acton",
                  "Lowell",
                  "Haverhill",
                  "Newburyport/Rockport"]
    
    routeTagPrefix = "CR-"

    routes = [(routeTagPrefix + str(i)) for i in xrange(1,13)]
    routeTitlesToKeys = {}
    for i in xrange(len(routes)):
        routeTitlesToKeys[routeNames[i]] = routes[i]


    x = dataFromMBTA.split("\n")
    csv = [each.split(",") for each in x]
    header = csv[0]
    csv = [csvToMap(header, line) for line in csv[1:]]
    routes = {}
    specialDirMapping = {}
    for mapping in csv:
        commuterRailRoute(routes, mapping, specialDirMapping, routeTitlesToKeys)

    #TODO: workarounds
    for route in routes.values():
        #TODO: put actual stops here
        for direction in route["directions"].values():
            direction["stops"] = []

    for routeTag, innerMapping in specialDirMapping.iteritems():
        for directionHash, innerInnerMapping in innerMapping.iteritems():
            for platformOrder, stop in innerInnerMapping.iteritems():
                routes[routeTag]["path"].append((stop["lat"], stop["lon"]))
                
        alreadyHandledDirections = {}
        trunkBranch = "Trunk"
        for directionHash in innerMapping.keys():
            direction, branch = directionHash.split("|")
            
            if direction in alreadyHandledDirections:
                continue
            if trunkBranch == branch:
                continue
            branchInnerMapping = innerMapping[directionHash]
            trunkDirectionHash = createCommuterRailDirectionHash(direction, trunkBranch)
            trunkInnerMapping = innerMapping[trunkDirectionHash]
            
            minBranchOrder = -1
            for order in branchInnerMapping.keys():
                if minBranchOrder == -1:
                    minBranchOrder = order
                else:
                    minBranchOrder = min(order, minBranchOrder)
            maxTrunkOrder = 0
            for order in trunkInnerMapping.keys():
                if order < minBranchOrder:
                    maxTrunkOrder = max(order, maxTrunkOrder)
            
            
            if minBranchOrder in branchInnerMapping and maxTrunkOrder in trunkInnerMapping:
                branchStop = branchInnerMapping[minBranchOrder]
                trunkStop = trunkInnerMapping[maxTrunkOrder]
                path = routes[routeTag]["path"]
                path.append((branchStop["lat"], branchStop["lon"]))
                path.append((trunkStop["lat"], trunkStop["lon"]))
    return routes
    
def commuterRailRoute(routes, routeCsv, specialDirMapping, routeTitlesToKeys):
    stopTagPrefix = "CRK-"
    routeTitle = routeCsv["route_long_name"]
    if routeTitle.endswith(" Line"):
        routeTitle = routeTitle[:-5]
    routeTag = routeTitlesToKeys[routeTitle]
        
    if routeTag not in routes:
        stops = {}
        directions = {}
        path = []
        routes[routeTag] = {"tag" : routeTag, "title": routeTitle, "color": "000000", "oppositeColor": "000000", "stops": stops, "directions": directions, "path" : path}
            
    route = routes[routeTag]

    #create stop
    stopTitle = routeCsv["stop_id"]
    stopTag = stopTagPrefix + stopTitle
    stop = {"tag" : stopTag, "title" : stopTitle, "lat" : routeCsv["stop_lat"], "lon" : routeCsv["stop_lon"], "platformOrder" : routeCsv["stop_sequence"], "branch" : routeCsv["Branch"]}
        
    route["stops"][stopTag] = stop

    if routeTag not in specialDirMapping:
        specialDirMapping[routeTag] = {}

    innerMapping = specialDirMapping[routeTag]

    combinedDirectionHash = createCommuterRailDirectionHash(routeCsv["direction_id"], routeCsv["Branch"])
    if combinedDirectionHash not in innerMapping:
        innerMapping[combinedDirectionHash] = {}
    innerInnerMapping = innerMapping[combinedDirectionHash]

    innerInnerMapping[stop["platformOrder"]] = stop
    
        
def createCommuterRailDirectionHash(direction, branch):
    return direction + "|" + branch

def subwayRoute(routes, routeCsv, specialDirMapping):
    routeTag = routeCsv["Line"]
    if routeTag not in routes:
        stops = {}
        directions = {}
        path = []
        routes[routeTag] = {"tag" : routeTag, "title" : routeTag, "color" : getColor(routeTag), "oppositeColor" : getColor("Blue"), "stops" : stops, "directions" : directions, "path" : path}
    
    route = routes[routeTag]
    stopTag = routeCsv["PlatformKey"]
    stop = {"tag": stopTag, "lat" : routeCsv["stop_lat"], "lon" : routeCsv["stop_lon"], "platformOrder" : routeCsv["PlatformOrder"], "title" : routeCsv["stop_name"], "branch" : routeCsv["Branch"]}
    route["stops"][stopTag] = stop
    
    if routeTag not in specialDirMapping:
        specialDirMapping[routeTag] = {}

    innerMapping = specialDirMapping[routeTag]
    
    combinedDirectionBranch = routeCsv["Direction"] + routeCsv["Branch"]
    if combinedDirectionBranch not in innerMapping:
        innerMapping[combinedDirectionBranch] = {}
    innerInnerMapping = innerMapping[combinedDirectionBranch]
    
    innerInnerMapping[stop["platformOrder"]] = stop

def subwayRoutes():
    url = "http://developer.mbta.com/RT_Archive/RealTimeHeavyRailKeys.csv"
    localFile, _ = urllib.urlretrieve(url)
    f = open(localFile)
    x = [each.strip() for each in f.readlines()]
    csv = [each.split(",") for each in x]
    header = csv[0]
    csv = [csvToMap(header, line) for line in csv[1:]]
    
    routes = {}
    specialDirMapping = {}
    for mapping in csv:
        subwayRoute(routes, mapping, specialDirMapping)

    RedNorthToAlewife = "RedNB0"
    RedNorthToAlewife2 = "RedNB1"
    RedSouthToBraintree = "RedSB0"
    RedSouthToAshmont = "RedSB1"
    BlueEastToWonderland = "BlueEB0"
    BlueWestToBowdoin = "BlueWB0"
    OrangeNorthToOakGrove = "OrangeNB0"
    OrangeSouthToForestHills = "OrangeSB0"
    
    #workarounds:
    routes["Red"]["directions"][RedNorthToAlewife] = {"tag": RedNorthToAlewife, "name": "North toward Alewife", "title": "", "route": "Red"}
    routes["Red"]["directions"][RedNorthToAlewife2] = {"tag": RedNorthToAlewife2, "name": "North toward Alewife", "title": "", "route": "Red"}
    routes["Red"]["directions"][RedSouthToBraintree] = {"tag": RedSouthToBraintree, "name": "South toward Braintree", "title": "", "route": "Red"}
    routes["Red"]["directions"][RedSouthToAshmont] = {"tag": RedSouthToAshmont, "name": "South toward Ashmont", "title": "", "route": "Red"}
    routes["Blue"]["directions"][BlueEastToWonderland] = {"tag": BlueEastToWonderland, "name": "East toward Wonderland", "title": "", "route": "Blue"}
    routes["Blue"]["directions"][BlueWestToBowdoin] = {"tag": BlueWestToBowdoin, "name": "West toward Bowdoin", "title": "", "route": "Blue"}
    routes["Orange"]["directions"][OrangeNorthToOakGrove] = {"tag": OrangeNorthToOakGrove, "name": "North toward Oak Grove", "title": "", "route": "Orange"}
    routes["Orange"]["directions"][OrangeSouthToForestHills] = {"tag": OrangeSouthToForestHills, "name": "South toward Forest Hills", "title": "", "route": "Orange"}

    # TODO: put actual stop sequences here
    for route in routes.values():
        for direction in route["directions"].values():
            direction["stops"] = []

    for routeTag, innerMapping in specialDirMapping.iteritems():
        for directionHash, innerInnerMapping in innerMapping.iteritems():
            for platformOrder, stop in innerInnerMapping.iteritems():
                lat = stop["lat"]
                lon = stop["lon"]
                routes[routeTag]["path"].append((lat, lon))

            #this is kind of a hack. We need to connect the southern branches of the red line to JFK manually
            if directionHash == "NBAshmont" or directionHash == "NBBraintree":
                jfkNorthBoundOrder = "5"
                jfkStation = innerMapping["NBTrunk"][jfkNorthBoundOrder]
                if jfkStation:
                    routes[routeTag]["path"].append((jfkStation["lat"], jfkStation["lon"]))


    return routes

def main():
    if len(sys.argv) != 3:
        sys.stderr.write("Usage: python routeconfig-to-java.py routeconfig.xml srcDirectory\n")
        exit(1)

    dom = get_dom(sys.argv[1])
    nextbusPrefix = "Nextbus"
    nextbusRoutes = nextbusToRoutes(dom)
    runPrepopulated(nextbusRoutes, nextbusPrefix)

    subwayPrefix = "Subway"
    routes = subwayRoutes()
    runPrepopulated(routes, subwayPrefix)

    commuterRailPrefix = "CommuterRail"
    routes = commuterRailRoutes()
    runPrepopulated(routes, commuterRailPrefix)
    #f = open(sys.argv[2] + "/boston/Bus/Map/data/NextbusPrepopulatedDirections.java", "wb")
    #runDirections(dom, f)

    

def test():
    x = """<route tag="1" title="1" color="330000" oppositeColor="ffffff" latMin="42.3297899" latMax="42.37513" lonMin="-71.11896" lonMax="-71.07354">
<stop tag="10590" title="banana" lat="42.3364699" lon="-71.07681" stopId="10590"/>
<stop tag="97" title="banana2" lat="42.3591799" lon="-71.09354" stopId="00097"/>
<stop tag="101" title="nana" lat="42.3629899" lon="-71.09949" stopId="00101"/>
</route>
"""

    dom = xml.dom.minidom.parseString(x)
    run(dom)

if __name__ == "__main__":
    main()
