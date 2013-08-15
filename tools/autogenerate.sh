#!/bin/bash
PROGNAME=$(basename $0)
GTFS_TO_SCHEDULE_DIR=../../gtfs-to-schedule

set -e
echo "Generating schema..."
python generate_schema.py > ../src/boston/Bus/Map/database/Schema.java
echo "Create tables..."
python create_tables.py > sql.dump
echo "Parsing commuter rail data..."
python commuterrail_tosql.py ../../gtfs 0 StationOrder.csv >> sql.dump
echo "Parsing subway data..."
python heavyrail_tosql.py ../../gtfs 12 >> sql.dump
echo "Parsing bus data..."
python tosql.py routeconfig.xml routeList 15 >> sql.dump
echo "Parsing alert data..."
python alerts_tosql.py routeList subwayRouteList commuterRailRouteList >> sql.dump
echo "Extracting GTFS data into timetables..."
rm -f $GTFS_TO_SCHEDULE_DIR/gtfs_out.sql
pushd $GTFS_TO_SCHEDULE_DIR
python make_database.py ../gtfs gtfs_out.sql
popd
cat $GTFS_TO_SCHEDULE_DIR/gtfs_out.sql >> sql.dump
#echo "Calculating bound times..."
#python calculate_times.py gtfs/stop_times.txt gtfs/trips.txt gtfs/calendar.txt >> sql.dump
echo "Dumping into sqlite..."
rm new.db* || true
sqlite3 new.db < sql.dump
gzip new.db
cp new.db.gz ../res/raw/databasegz
echo "Done!"

