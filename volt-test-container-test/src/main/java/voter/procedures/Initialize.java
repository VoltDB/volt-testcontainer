/*
 * Copyright (C) 2024 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package voter.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

/**
 * The Initialize class extends VoltProcedure and provides an initialization mechanism
 * for a database containing contestants and mappings of area codes to states.
 * This class ensures that the database is populated with initial data if it hasn't been initialized already.
 */
public class Initialize extends VoltProcedure
{
    /**
     * SQL statement to verify if the 'contestants' table has any records.
     * Used to determine if the database has already been initialized.
     */
    public final SQLStmt checkStmt = new SQLStmt("SELECT COUNT(*) FROM contestants;");

    /**
     * An SQL statement used to insert values into the area_code_state table.
     * This statement expects two parameters to be filled before execution:
     * 1. An area code.
     * 2. A corresponding state.
     */
    public final SQLStmt insertACSStmt = new SQLStmt("INSERT INTO area_code_state VALUES (?,?);");

    /**
     * SQL statement for inserting a new contestant into the contestants table.
     * The query inserts values for contestant_name and contestant_number.
     * The values are provided as parameters during statement execution.
     */
    public final SQLStmt insertContestantStmt = new SQLStmt("INSERT INTO contestants (contestant_name, contestant_number) VALUES (?, ?);");

    /**
     * An array of area codes used in various operations within the Initialize class.
     * This array is declared as a public static final constant and includes area codes
     * from different regions.
     */
    public static final short[] areaCodes = new short[]{
            907, 205, 256, 334, 251, 870, 501, 479, 480, 602, 623, 928, 520, 341, 764, 628, 831, 925,
            909, 562, 661, 510, 650, 949, 760, 415, 951, 209, 669, 408, 559, 626, 442, 530, 916, 627,
            714, 707, 310, 323, 213, 424, 747, 818, 858, 935, 619, 805, 369, 720, 303, 970, 719, 860,
            203, 959, 475, 202, 302, 689, 407, 239, 850, 727, 321, 754, 954, 927, 352, 863, 386, 904,
            561, 772, 786, 305, 941, 813, 478, 770, 470, 404, 762, 706, 678, 912, 229, 808, 515, 319,
            563, 641, 712, 208, 217, 872, 312, 773, 464, 708, 224, 847, 779, 815, 618, 309, 331, 630,
            317, 765, 574, 260, 219, 812, 913, 785, 316, 620, 606, 859, 502, 270, 504, 985, 225, 318,
            337, 774, 508, 339, 781, 857, 617, 978, 351, 413, 443, 410, 301, 240, 207, 517, 810, 278,
            679, 313, 586, 947, 248, 734, 269, 989, 906, 616, 231, 612, 320, 651, 763, 952, 218, 507,
            636, 660, 975, 816, 573, 314, 557, 417, 769, 601, 662, 228, 406, 336, 252, 984, 919, 980,
            910, 828, 704, 701, 402, 308, 603, 908, 848, 732, 551, 201, 862, 973, 609, 856, 575, 957,
            505, 775, 702, 315, 518, 646, 347, 212, 718, 516, 917, 845, 631, 716, 585, 607, 914, 216,
            330, 234, 567, 419, 440, 380, 740, 614, 283, 513, 937, 918, 580, 405, 503, 541, 971, 814,
            717, 570, 878, 835, 484, 610, 267, 215, 724, 412, 401, 843, 864, 803, 605, 423, 865, 931,
            615, 901, 731, 254, 325, 713, 940, 817, 430, 903, 806, 737, 512, 361, 210, 979, 936, 409,
            972, 469, 214, 682, 832, 281, 830, 956, 432, 915, 435, 801, 385, 434, 804, 757, 703, 571,
            276, 236, 540, 802, 509, 360, 564, 206, 425, 253, 715, 920, 262, 414, 608, 304, 307};

    /**
     * An array of two-letter state codes representing various states in the United States. Each entry in the array corresponds to a state.
     */
    public static final String[] states = new String[]{
            "AK", "AL", "AL", "AL", "AL", "AR", "AR", "AR", "AZ", "AZ", "AZ", "AZ", "AZ", "CA", "CA",
            "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA",
            "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA", "CA",
            "CA", "CA", "CA", "CA", "CO", "CO", "CO", "CO", "CT", "CT", "CT", "CT", "DC", "DE", "FL",
            "FL", "FL", "FL", "FL", "FL", "FL", "FL", "FL", "FL", "FL", "FL", "FL", "FL", "FL", "FL",
            "FL", "FL", "FL", "GA", "GA", "GA", "GA", "GA", "GA", "GA", "GA", "GA", "HI", "IA", "IA",
            "IA", "IA", "IA", "ID", "IL", "IL", "IL", "IL", "IL", "IL", "IL", "IL", "IL", "IL", "IL",
            "IL", "IL", "IL", "IN", "IN", "IN", "IN", "IN", "IN", "KS", "KS", "KS", "KS", "KY", "KY",
            "KY", "KY", "LA", "LA", "LA", "LA", "LA", "MA", "MA", "MA", "MA", "MA", "MA", "MA", "MA",
            "MA", "MD", "MD", "MD", "MD", "ME", "MI", "MI", "MI", "MI", "MI", "MI", "MI", "MI", "MI",
            "MI", "MI", "MI", "MI", "MI", "MN", "MN", "MN", "MN", "MN", "MN", "MN", "MO", "MO", "MO",
            "MO", "MO", "MO", "MO", "MO", "MS", "MS", "MS", "MS", "MT", "NC", "NC", "NC", "NC", "NC",
            "NC", "NC", "NC", "ND", "NE", "NE", "NH", "NJ", "NJ", "NJ", "NJ", "NJ", "NJ", "NJ", "NJ",
            "NJ", "NM", "NM", "NM", "NV", "NV", "NY", "NY", "NY", "NY", "NY", "NY", "NY", "NY", "NY",
            "NY", "NY", "NY", "NY", "NY", "OH", "OH", "OH", "OH", "OH", "OH", "OH", "OH", "OH", "OH",
            "OH", "OH", "OK", "OK", "OK", "OR", "OR", "OR", "PA", "PA", "PA", "PA", "PA", "PA", "PA",
            "PA", "PA", "PA", "PA", "RI", "SC", "SC", "SC", "SD", "TN", "TN", "TN", "TN", "TN", "TN",
            "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX",
            "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX", "TX", "UT", "UT", "UT", "VA", "VA",
            "VA", "VA", "VA", "VA", "VA", "VA", "VT", "WA", "WA", "WA", "WA", "WA", "WA", "WI", "WI",
            "WI", "WI", "WI", "WV", "WY"};

    /**
     * Initializes the contestant data and inserts contestant information into the database if not already initialized.
     *
     * @param maxContestants The maximum number of contestants to be initialized.
     * @param contestants A comma-separated string of contestant names.
     * @return The total count of contestants that have been initialized.
     */
    public long run(int maxContestants, String contestants) {

        String[] contestantArray = contestants.split(",");

        voltQueueSQL(checkStmt, EXPECT_SCALAR_LONG);
        long existingContestantCount = voltExecuteSQL()[0].asScalarLong();

        // if the data is initialized, return the contestant count
        if (existingContestantCount != 0)
            return existingContestantCount;

        // initialize the data

        for (int i=0; i < maxContestants; i++)
            voltQueueSQL(insertContestantStmt, EXPECT_SCALAR_MATCH(1), contestantArray[i], i+1);
        voltExecuteSQL();

        for(int i=0;i<areaCodes.length;i++)
            voltQueueSQL(insertACSStmt, EXPECT_SCALAR_MATCH(1), areaCodes[i], states[i]);
        voltExecuteSQL();

        return maxContestants;
    }
}
