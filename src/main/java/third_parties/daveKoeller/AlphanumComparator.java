/*
 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers.  Instead of sorting numbers in ASCII order like
 * a standard sort, this algorithm sorts numbers in numeric order.
 *
 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
 *
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package third_parties.daveKoeller;

import com.owncloud.android.datamodel.OCFile;

import java.io.File;
import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;

/*
 * This is an updated version with enhancements made by Daniel Migowski, Andre Bogus, and David Koelle
 *  * 
 * To convert to use Templates (Java 1.5+):
 * - Change "implements Comparator" to "implements Comparator<String>"
 * - Change "compare(Object o1, Object o2)" to "compare(String s1, String s2)"
 * - Remove the type checking and casting in compare().
 * 
 * To use this class:
 * Use the static "sort" method from the java.util.Collections class:
 * Collections.sort(your list, new AlphanumComparator());
 * 
 * Adapted to fit 
 * https://github.com/nextcloud/server/blob/9a4253ef7c34f9dc71a6a9f7828a10df769f0c32/tests/lib/NaturalSortTest.php
 * by Tobias Kaminsky
 */
public class AlphanumComparator<T> implements Comparator<T>, Serializable {
    private boolean isDigit(char ch) {
        return ch >= 48 && ch <= 57;
    }

    private boolean isSpecialChar(char ch) {
        return ch <= 47 || ch >= 58 && ch <= 64 || ch >= 91 && ch <= 96 || ch >= 123 && ch <= 126;
    }

    /**
     * Length of string is passed in for improved efficiency (only need to calculate it once)
     **/
    private String getChunk(String string, int stringLength, int marker) {
        StringBuilder chunk = new StringBuilder();
        char c = string.charAt(marker);
        chunk.append(c);
        marker++;
        if (isDigit(c)) {
            while (marker < stringLength) {
                c = string.charAt(marker);
                if (!isDigit(c)) {
                    break;
                }
                chunk.append(c);
                marker++;
            }
        } else if (!isSpecialChar(c)) {
            while (marker < stringLength) {
                c = string.charAt(marker);
                if (isDigit(c) || isSpecialChar(c)) {
                    break;
                }
                chunk.append(c);
                marker++;
            }
        }
        return chunk.toString();
    }

    public int compare(OCFile o1, OCFile o2) {
        String s1 = o1.getFileName();
        String s2 = o2.getFileName();

        return compare(s1, s2);
    }

    public int compare(File f1, File f2) {
        String s1 = f1.getPath();
        String s2 = f2.getPath();

        return compare(s1, s2);
    }

    public int compare(T t1, T t2) {
        return compare(t1.toString(), t2.toString());
    }

    public int compare(String s1, String s2) {
        int thisMarker = 0;
        int thatMarker = 0;
        int s1Length = s1.length();
        int s2Length = s2.length();

        while (thisMarker < s1Length && thatMarker < s2Length) {
            String thisChunk = getChunk(s1, s1Length, thisMarker);
            thisMarker += thisChunk.length();

            String thatChunk = getChunk(s2, s2Length, thatMarker);
            thatMarker += thatChunk.length();

            // If both chunks contain numeric characters, sort them numerically
            int result = 0;
            if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
                // extract digits
                int thisChunkZeroCount = 0;
                boolean zero = true;
                int countThis = 0;
                while (countThis < (thisChunk.length()) && isDigit(thisChunk.charAt(countThis))) {
                    if (zero) {
                        if (Character.getNumericValue(thisChunk.charAt(countThis)) == 0) {
                            thisChunkZeroCount++;
                        } else {
                            zero = false;
                        }
                    }
                    countThis++;
                }


                int thatChunkZeroCount = 0;
                int countThat = 0;
                zero = true;
                while (countThat < (thatChunk.length()) && isDigit(thatChunk.charAt(countThat))) {
                    if (zero) {
                        if (Character.getNumericValue(thatChunk.charAt(countThat)) == 0) {
                            thatChunkZeroCount++;
                        } else {
                            zero = false;
                        }
                    }
                    countThat++;
                }

                try {
                    long thisChunkValue = Long.parseLong(thisChunk.substring(0, countThis));
                    long thatChunkValue = Long.parseLong(thatChunk.substring(0, countThat));

                    result = Long.compare(thisChunkValue, thatChunkValue);
                } catch (NumberFormatException exception) {
                    result = thisChunk.substring(0, countThis).compareTo(thatChunk.substring(0, countThat));
                }
                
                if (result == 0) {
                    // value is equal, compare leading zeros
                    result = Integer.compare(thisChunkZeroCount, thatChunkZeroCount);

                    if (result != 0) {
                        return result;
                    }
                } else {
                    return result;
                }
            } else if (isSpecialChar(thisChunk.charAt(0)) && isSpecialChar(thatChunk.charAt(0))) {
                for (int i = 0; i < thisChunk.length(); i++) {
                    if (thisChunk.charAt(i) == '.') {
                        return -1;
                    } else if (thatChunk.charAt(i) == '.') {
                        return 1;
                    } else {
                        result = thisChunk.charAt(i) - thatChunk.charAt(i);
                        if (result != 0) {
                            return result;
                        }
                    }
                }
            } else if (isSpecialChar(thisChunk.charAt(0)) && !isSpecialChar(thatChunk.charAt(0))) {
                return -1;
            } else if (!isSpecialChar(thisChunk.charAt(0)) && isSpecialChar(thatChunk.charAt(0))) {
                return 1;
            } else {
                result = Collator.getInstance().compare(thisChunk, thatChunk);
            }

            if (result != 0) {
                return result;
            }
        }

        return s1Length - s2Length;
    }
}
