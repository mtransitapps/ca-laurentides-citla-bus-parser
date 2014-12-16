package org.mtransit.parser.ca_laurentides_citla_bus;

import java.util.regex.Pattern;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http://www.amt.qc.ca/developers/
// http://www.amt.qc.ca/xdata/citla/google_transit.zip
public class LaurentidesCITLABusAgencyTools extends DefaultAgencyTools {

	public static final String ROUTE_TYPE_FILTER = "3"; // bus only

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-laurentides-citla-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LaurentidesCITLABusAgencyTools().start(args);
	}

	@Override
	public void start(String[] args) {
		System.out.printf("Generating CITLA bus data...\n");
		long start = System.currentTimeMillis();
		super.start(args);
		System.out.printf("Generating CITLA bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (ROUTE_TYPE_FILTER != null && !gRoute.route_type.equals(ROUTE_TYPE_FILTER)) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	private static final Pattern P1METRO = Pattern.compile("(\\(métro )", Pattern.CASE_INSENSITIVE);
	private static final String P1METRO_REPLACEMENT = "\\(";

	private static final Pattern SECTEUR = Pattern.compile("(secteur[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String SECTEUR_REPLACEMENT = "";

	private static final Pattern DASH_DES = Pattern.compile("(\\- de[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String DASH_DES_REPLACEMENT = "- ";

	private static final Pattern BOISBRIAND_SUD_VERS_NORD = Pattern.compile("(Boisbriand Sud Vers Boisbriand Nord)", Pattern.CASE_INSENSITIVE);
	private static final String BOISBRIAND_SUD_VERS_NORD_REPLACEMENT = "Boisbriand Sud => Nord";

	private static final Pattern BOISBRIAND_NORD_VERS_SUD = Pattern.compile("(Boisbriand Nord Vers Boisbriand Sud)", Pattern.CASE_INSENSITIVE);
	private static final String BOISBRIAND_NORD_VERS_SUD_REPLACEMENT = "Boisbriand Nord => Sud";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.route_long_name;
		routeLongName = MSpec.SAINT.matcher(routeLongName).replaceAll(MSpec.SAINT_REPLACEMENT);
		routeLongName = MSpec.POINT.matcher(routeLongName).replaceAll(MSpec.POINT_REPLACEMENT);
		routeLongName = MSpec.ET.matcher(routeLongName).replaceAll(MSpec.ET_REPLACEMENT);
		routeLongName = P1METRO.matcher(routeLongName).replaceAll(P1METRO_REPLACEMENT);
		routeLongName = SECTEUR.matcher(routeLongName).replaceAll(SECTEUR_REPLACEMENT);
		routeLongName = DASH_DES.matcher(routeLongName).replaceAll(DASH_DES_REPLACEMENT);
		routeLongName = BOISBRIAND_SUD_VERS_NORD.matcher(routeLongName).replaceAll(BOISBRIAND_SUD_VERS_NORD_REPLACEMENT);
		routeLongName = BOISBRIAND_NORD_VERS_SUD.matcher(routeLongName).replaceAll(BOISBRIAND_NORD_VERS_SUD_REPLACEMENT);
		return MSpec.cleanLabel(routeLongName);
	}

	private static final String ROUTE_COLOR = "005984";

	@Override
	public String getRouteColor(GRoute gRoute) {
		return ROUTE_COLOR;
	}

	private static final String ROUTE_TEXT_COLOR = "FFFFFF";

	@Override
	public String getRouteTextColor(GRoute gRoute) {
		return ROUTE_TEXT_COLOR;
	}

	@Override
	public void setTripHeadsign(MRoute route, MTrip mTrip, GTrip gTrip) {
		String stationName = cleanTripHeadsign(gTrip.trip_headsign);
		int directionId = Integer.valueOf(gTrip.direction_id);
		mTrip.setHeadsignString(stationName, directionId);
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = "";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(DIRECTION_REPLACEMENT);
		tripHeadsign = SECTEUR.matcher(tripHeadsign).replaceAll(SECTEUR_REPLACEMENT);
		tripHeadsign = MSpec.POINT.matcher(tripHeadsign).replaceAll(MSpec.POINT_REPLACEMENT);
		return MSpec.cleanLabelFR(tripHeadsign);
	}

	private static final Pattern STATION_DE_METRO = Pattern.compile("(station de métro )", Pattern.CASE_INSENSITIVE);
	private static final String STATION_DE_METRO_REPLACEMENT = "station ";

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[] { START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE };

	private static final Pattern[] SPACE_FACES = new Pattern[] { SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE };

	private static final Pattern AVENUE = Pattern.compile("( avenue)", Pattern.CASE_INSENSITIVE);
	private static final String AVENUE_REPLACEMENT = " av.";

	private static final Pattern BOULEVARD = Pattern.compile("( boulevard)", Pattern.CASE_INSENSITIVE);
	private static final String BOULEVARD_REPLACEMENT = " boul.";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STATION_DE_METRO.matcher(gStopName).replaceAll(STATION_DE_METRO_REPLACEMENT);
		gStopName = AVENUE.matcher(gStopName).replaceAll(AVENUE_REPLACEMENT);
		gStopName = BOULEVARD.matcher(gStopName).replaceAll(BOULEVARD_REPLACEMENT);
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, MSpec.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, MSpec.SPACE);
		return super.cleanStopNameFR(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.valueOf(gStop.stop_code); // using stop code as stop ID
	}

	@Override
	public String getStopCode(GStop gStop) {
		return gStop.stop_id; // using stop ID as stop code
	}

}
