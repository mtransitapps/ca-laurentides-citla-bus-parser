package org.mtransit.parser.ca_laurentides_citla_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://exo.quebec/en/about/open-data
// https://exo.quebec/xdata/citla/google_transit.zip
public class LaurentidesCITLABusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-laurentides-citla-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LaurentidesCITLABusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating CITLA bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating CITLA bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
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
			Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
			if (matcher.find()) {
				int digits = Integer.parseInt(matcher.group());
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

	private static final Pattern SECTEUR = Pattern.compile("(secteur[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String SECTEUR_REPLACEMENT = "";

	private static final Pattern DASH_DES = Pattern.compile("(- de[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String DASH_DES_REPLACEMENT = "- ";

	private static final Pattern BOISBRIAND_SUD_VERS_NORD = Pattern.compile("(Boisbriand Sud Vers Boisbriand Nord)", Pattern.CASE_INSENSITIVE);
	private static final String BOISBRIAND_SUD_VERS_NORD_REPLACEMENT = "Boisbriand Sud => Nord";

	private static final Pattern BOISBRIAND_NORD_VERS_SUD = Pattern.compile("(Boisbriand Nord Vers Boisbriand Sud)", Pattern.CASE_INSENSITIVE);
	private static final String BOISBRIAND_NORD_VERS_SUD_REPLACEMENT = "Boisbriand Nord => Sud";

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.POINT.matcher(routeLongName).replaceAll(CleanUtils.POINT_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_ET.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_ET_REPLACEMENT);
		routeLongName = P1METRO.matcher(routeLongName).replaceAll(P1METRO_REPLACEMENT);
		routeLongName = SECTEUR.matcher(routeLongName).replaceAll(SECTEUR_REPLACEMENT);
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

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(103L, new RouteTripSpec(103L, // BECAUSE same trip head-sign for 2 directions
				0, MTrip.HEADSIGN_TYPE_STRING, "Méga Centre / Carref. Du Nord", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Gare St-Jérôme") //
				.addTripSort(0, //
						Arrays.asList( //
								"80501", // Gare Saint-Jérôme
								"80593" // Carrefour du Nord
						)) //
				.addTripSort(1, //
						Arrays.asList( //
								"80593", // Carrefour du Nord
								"80501" // Gare Saint-Jérôme
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 8L) {
			if (Arrays.asList( //
					"Terminus / St-Eustache" + " Via Le Carref.", //
					"Terminus / St-Eustache" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Terminus / St-Eustache", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Métro / Montmorency" + " Via Le Carref.", //
					"Métro / Montmorency" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Métro / Montmorency", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 9L) {
			if (Arrays.asList( //
					"Lafontaine / Via Gare " + "St-Jérôme", //
					"St-Jérôme" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("St-Jérôme", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 12L) {
			if (Arrays.asList( //
					"Gare Rosemère", //
					"Laval" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Laval", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 80L) {
			if (Arrays.asList( //
					"Pointe-Calumet / Via 59e Av.", //
					"Terminus / St-Eustache" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Terminus / St-Eustache", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 88L) {
			if (Arrays.asList( //
					"Express / " + "Terminus / St-Eustache", //
					"Terminus / St-Eustache" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Terminus / St-Eustache", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final Pattern EXPRESS_ = Pattern.compile("(express )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = EXPRESS_.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = SECTEUR.matcher(tripHeadsign).replaceAll(SECTEUR_REPLACEMENT);
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = CleanUtils.POINT.matcher(tripHeadsign).replaceAll(CleanUtils.POINT_REPLACEMENT);
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

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
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
			return StringUtils.EMPTY;
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
