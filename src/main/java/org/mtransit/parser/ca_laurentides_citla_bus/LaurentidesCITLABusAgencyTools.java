package org.mtransit.parser.ca_laurentides_citla_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.Constants.EMPTY;
import static org.mtransit.parser.Constants.SPACE_;

// https://exo.quebec/en/about/open-data
// https://exo.quebec/xdata/citla/google_transit.zip
public class LaurentidesCITLABusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new LaurentidesCITLABusAgencyTools().start(args);
	}

	@Override
	public String getAgencyName() {
		return "exo Laurentides";
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final String T = "T";

	private static final long RID_STARTS_WITH_T = 20_000L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		//noinspection deprecation
		if (!Utils.isDigitsOnly(gRoute.getRouteId())) {
			final Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
			if (matcher.find()) {
				final int digits = Integer.parseInt(matcher.group());
				if (gRoute.getRouteShortName().startsWith(T)) {
					return RID_STARTS_WITH_T + digits;
				}
			}
			throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute.toStringPlus());
		}
		return super.getRouteId(gRoute);
	}

	private static final Pattern P1METRO = Pattern.compile("(\\(métro )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final String P1METRO_REPLACEMENT = "\\(";

	private static final Pattern DASH_DES = Pattern.compile("(- de[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String DASH_DES_REPLACEMENT = "- ";

	private static final Pattern BOISBRIAND_SUD_VERS_NORD = Pattern.compile("(Boisbriand Sud Vers Boisbriand Nord)", Pattern.CASE_INSENSITIVE);
	private static final String BOISBRIAND_SUD_VERS_NORD_REPLACEMENT = "Boisbriand Sud => Nord";

	private static final Pattern BOISBRIAND_NORD_VERS_SUD = Pattern.compile("(Boisbriand Nord Vers Boisbriand Sud)", Pattern.CASE_INSENSITIVE);
	private static final String BOISBRIAND_NORD_VERS_SUD_REPLACEMENT = "Boisbriand Nord => Sud";

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.POINT.matcher(routeLongName).replaceAll(CleanUtils.POINT_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_ET.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
		routeLongName = P1METRO.matcher(routeLongName).replaceAll(P1METRO_REPLACEMENT);
		routeLongName = DASH_DES.matcher(routeLongName).replaceAll(DASH_DES_REPLACEMENT);
		routeLongName = BOISBRIAND_SUD_VERS_NORD.matcher(routeLongName).replaceAll(BOISBRIAND_SUD_VERS_NORD_REPLACEMENT);
		routeLongName = BOISBRIAND_NORD_VERS_SUD.matcher(routeLongName).replaceAll(BOISBRIAND_NORD_VERS_SUD_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "1F1F1F"; // DARK GRAY (from GTFS)

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern EXPRESS_ = Pattern.compile("(express )", Pattern.CASE_INSENSITIVE);

	private static final Pattern _DASH_ = Pattern.compile("( - )");
	private static final String _DASH_REPLACEMENT = "<>"; // form<>to

	private static final Pattern CIVIQUE_ = Pattern.compile("((^|\\W)(" + "civique #?([\\d]+)" + ")(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String CIVIQUE_REPLACEMENT = "$2" + "#$4" + "$5";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = _DASH_.matcher(tripHeadsign).replaceAll(_DASH_REPLACEMENT); // from - to => form<>to
		tripHeadsign = EXPRESS_.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CIVIQUE_.matcher(tripHeadsign).replaceAll(CIVIQUE_REPLACEMENT);
		tripHeadsign = CleanUtils.keepToFR(tripHeadsign);
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = CleanUtils.POINT.matcher(tripHeadsign).replaceAll(CleanUtils.POINT_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(Locale.FRENCH, tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[]{START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE};

	private static final Pattern[] SPACE_FACES = new Pattern[]{SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE};

	private static final Pattern DEVANT_ = CleanUtils.cleanWordsFR("devant");

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = _DASH_.matcher(gStopName).replaceAll(SPACE_);
		gStopName = DEVANT_.matcher(gStopName).replaceAll(EMPTY);
		gStopName = CIVIQUE_.matcher(gStopName).replaceAll(CIVIQUE_REPLACEMENT);
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, CleanUtils.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, CleanUtils.SPACE);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	private static final String ZERO = "0";

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		if (ZERO.equals(gStop.getStopCode())) {
			return EMPTY;
		}
		return super.getStopCode(gStop);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(@NotNull GStop gStop) {
		String stopCode = getStopCode(gStop);
		if (stopCode.length() > 0 && Utils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		//noinspection deprecation
		final String stopId1 = gStop.getStopId();
		Matcher matcher = DIGITS.matcher(stopId1);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (stopId1.startsWith("BLA")) {
				stopId = 100_000;
			} else if (stopId1.startsWith("SEU")) {
				stopId = 200_000;
			} else if (stopId1.startsWith("SJM")) {
				stopId = 300_000;
			} else if (stopId1.startsWith("ROS")) {
				stopId = 400_000;
			} else if (stopId1.startsWith("TER")) {
				stopId = 500_000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (start with) %s!", gStop);
			}
			if (stopId1.endsWith("A")) {
				stopId += 1_000;
			} else if (stopId1.endsWith("B")) {
				stopId += 2_000;
			} else if (stopId1.endsWith("C")) {
				stopId += 3_000;
			} else if (stopId1.endsWith("D")) {
				stopId += 4_000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (end with) %s!", gStop);
			}
			return stopId + digits;
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}
}
