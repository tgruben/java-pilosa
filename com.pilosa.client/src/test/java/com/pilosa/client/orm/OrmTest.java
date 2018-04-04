/*
 * Copyright 2017 Pilosa Corp.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package com.pilosa.client.orm;

import com.pilosa.client.UnitTest;
import com.pilosa.client.exceptions.PilosaException;
import com.pilosa.client.exceptions.ValidationException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

@Category(UnitTest.class)
public class OrmTest {
    private Index sampleIndex = Index.withName("sample-db");
    private Frame sampleFrame = sampleIndex.frame("sample-frame");
    private Index projectIndex;
    private Frame collabFrame;

    {
        this.projectIndex = Index.withName("project-db");
        FrameOptions collabFrameOptions = FrameOptions.withDefaults();
        this.collabFrame = projectIndex.frame("collaboration", collabFrameOptions);
    }

    @Test
    public void pqlQueryCreate() {
        PqlBaseQuery pql = new PqlBaseQuery("Bitmap(frame='foo', row=10)");
        assertEquals("Bitmap(frame='foo', row=10)", pql.serialize());
        assertEquals(null, pql.getIndex());
    }

    @Test
    public void pqlBitmapQueryCreate() {
        PqlBitmapQuery pql = new PqlBitmapQuery("Bitmap(frame='foo', row=10)");
        assertEquals("Bitmap(frame='foo', row=10)", pql.serialize());
        assertEquals(null, pql.getIndex());
    }

    @Test
    public void batchTest() {
        PqlBatchQuery b = sampleIndex.batchQuery();
        b.add(sampleFrame.bitmap(44));
        b.add(sampleFrame.bitmap(10101));
        assertEquals(
                "Bitmap(row=44, frame='sample-frame')Bitmap(row=10101, frame='sample-frame')",
                b.serialize());
    }

    @Test
    public void batchWithCapacityTest() {
        PqlBatchQuery b = projectIndex.batchQuery(3);
        b.add(collabFrame.bitmap(2));
        b.add(collabFrame.setBit(20, 40));
        b.add(collabFrame.topN(2));
        assertEquals(
                "Bitmap(row=2, frame='collaboration')" +
                        "SetBit(row=20, frame='collaboration', col=40)" +
                        "TopN(frame='collaboration', n=2, inverse=false)",
                b.serialize());
    }

    @Test(expected = PilosaException.class)
    public void batchAddFailsForDifferentDbsTest() {
        PqlBatchQuery b = projectIndex.batchQuery();
        b.add(sampleFrame.bitmap(1));
    }

    @Test
    public void bitmapTest() {
        PqlBaseQuery qry1 = sampleFrame.bitmap(5);
        assertEquals(
                "Bitmap(row=5, frame='sample-frame')",
                qry1.serialize());

        PqlBaseQuery qry2 = collabFrame.bitmap(10);
        assertEquals(
                "Bitmap(row=10, frame='collaboration')",
                qry2.serialize());
        PqlBaseQuery qry3 = sampleFrame.bitmap("b7feb014-8ea7-49a8-9cd8-19709161ab63");
        assertEquals(
                "Bitmap(row='b7feb014-8ea7-49a8-9cd8-19709161ab63', frame='sample-frame')",
                qry3.serialize());
    }

    @Test
    public void inverseBitmapTest() {
        FrameOptions options = FrameOptions.builder()
                .setInverseEnabled(true)
                .build();
        Frame f1 = this.projectIndex.frame("f1-inversable", options);
        PqlBaseQuery qry = f1.inverseBitmap(5);
        assertEquals(
                "Bitmap(col=5, frame='f1-inversable')",
                qry.serialize()
        );
        PqlBaseQuery qry2 = f1.inverseBitmap("b7feb014-8ea7-49a8-9cd8-19709161ab63");
        assertEquals(
                "Bitmap(col='b7feb014-8ea7-49a8-9cd8-19709161ab63', frame='f1-inversable')",
                qry2.serialize());
    }

    @Test
    public void setBitTest() {
        PqlQuery qry1 = sampleFrame.setBit(5, 10);
        assertEquals(
                "SetBit(row=5, frame='sample-frame', col=10)",
                qry1.serialize());

        PqlQuery qry2 = collabFrame.setBit(10, 20);
        assertEquals(
                "SetBit(row=10, frame='collaboration', col=20)",
                qry2.serialize());
        PqlQuery qry3 = sampleFrame.setBit("b7feb014-8ea7-49a8-9cd8-19709161ab63", "some_id");
        assertEquals(
                "SetBit(row='b7feb014-8ea7-49a8-9cd8-19709161ab63', frame='sample-frame', col='some_id')",
                qry3.serialize());

    }

    @Test
    public void setBitWithTimestampTest() {
        Calendar timestamp = Calendar.getInstance();
        timestamp.set(2017, Calendar.APRIL, 24, 12, 14);
        PqlQuery qry = collabFrame.setBit(10, 20, timestamp.getTime());
        assertEquals(
                "SetBit(row=10, frame='collaboration', col=20, timestamp='2017-04-24T12:14')",
                qry.serialize());
        PqlQuery qry2 = collabFrame.setBit("b7feb014-8ea7-49a8-9cd8-19709161ab63", "some", timestamp.getTime());
        assertEquals(
                "SetBit(row='b7feb014-8ea7-49a8-9cd8-19709161ab63', frame='collaboration', col='some', timestamp='2017-04-24T12:14')",
                qry2.serialize());
        PqlQuery qry3 = sampleFrame.clearBit("b7feb014-8ea7-49a8-9cd8-19709161ab63", "some_id");
        assertEquals(
                "ClearBit(row='b7feb014-8ea7-49a8-9cd8-19709161ab63', frame='sample-frame', col='some_id')",
                qry3.serialize());

    }

    @Test
    public void clearBitTest() {
        PqlQuery qry1 = sampleFrame.clearBit(5, 10);
        assertEquals(
                "ClearBit(row=5, frame='sample-frame', col=10)",
                qry1.serialize());

        PqlQuery qry2 = collabFrame.clearBit(10, 20);
        assertEquals(
                "ClearBit(row=10, frame='collaboration', col=20)",
                qry2.serialize());
    }

    @Test
    public void unionTest() {
        PqlBitmapQuery b1 = sampleFrame.bitmap(10);
        PqlBitmapQuery b2 = sampleFrame.bitmap(20);
        PqlBitmapQuery b3 = sampleFrame.bitmap(42);
        PqlBitmapQuery b4 = collabFrame.bitmap(2);

        PqlBaseQuery q1 = sampleIndex.union(b1, b2);
        assertEquals(
                "Union(Bitmap(row=10, frame='sample-frame'), Bitmap(row=20, frame='sample-frame'))",
                q1.serialize());

        PqlBaseQuery q2 = sampleIndex.union(b1, b2, b3);
        assertEquals(
                "Union(Bitmap(row=10, frame='sample-frame'), Bitmap(row=20, frame='sample-frame'), Bitmap(row=42, frame='sample-frame'))",
                q2.serialize());

        PqlBaseQuery q3 = sampleIndex.union(b1, b4);
        assertEquals(
                "Union(Bitmap(row=10, frame='sample-frame'), Bitmap(row=2, frame='collaboration'))",
                q3.serialize());
    }

    @Test
    public void union0Test() {
        PqlBitmapQuery q = sampleIndex.union();
        assertEquals("Union()", q.serialize());
    }

    @Test
    public void union1Test() {
        PqlBitmapQuery q = sampleIndex.union(sampleFrame.bitmap(10));
        assertEquals("Union(Bitmap(row=10, frame='sample-frame'))", q.serialize());
    }

    @Test
    public void intersectTest() {
        PqlBitmapQuery b1 = sampleFrame.bitmap(10);
        PqlBitmapQuery b2 = sampleFrame.bitmap(20);
        PqlBitmapQuery b3 = sampleFrame.bitmap(42);
        PqlBitmapQuery b4 = collabFrame.bitmap(2);

        PqlBaseQuery q1 = sampleIndex.intersect(b1, b2);
        assertEquals(
                "Intersect(Bitmap(row=10, frame='sample-frame'), Bitmap(row=20, frame='sample-frame'))",
                q1.serialize());

        PqlBaseQuery q2 = sampleIndex.intersect(b1, b2, b3);
        assertEquals(
                "Intersect(Bitmap(row=10, frame='sample-frame'), Bitmap(row=20, frame='sample-frame'), Bitmap(row=42, frame='sample-frame'))",
                q2.serialize());

        PqlBaseQuery q3 = sampleIndex.intersect(b1, b4);
        assertEquals(
                "Intersect(Bitmap(row=10, frame='sample-frame'), Bitmap(row=2, frame='collaboration'))",
                q3.serialize());
    }

    @Test
    public void differenceTest() {
        PqlBitmapQuery b1 = sampleFrame.bitmap(10);
        PqlBitmapQuery b2 = sampleFrame.bitmap(20);
        PqlBitmapQuery b3 = sampleFrame.bitmap(42);
        PqlBitmapQuery b4 = collabFrame.bitmap(2);

        PqlBaseQuery q1 = sampleIndex.difference(b1, b2);
        assertEquals(
                "Difference(Bitmap(row=10, frame='sample-frame'), Bitmap(row=20, frame='sample-frame'))",
                q1.serialize());

        PqlBaseQuery q2 = sampleIndex.difference(b1, b2, b3);
        assertEquals(
                "Difference(Bitmap(row=10, frame='sample-frame'), Bitmap(row=20, frame='sample-frame'), Bitmap(row=42, frame='sample-frame'))",
                q2.serialize());

        PqlBaseQuery q3 = sampleIndex.difference(b1, b4);
        assertEquals(
                "Difference(Bitmap(row=10, frame='sample-frame'), Bitmap(row=2, frame='collaboration'))",
                q3.serialize());
    }

    @Test
    public void xorTest() {
        PqlBitmapQuery b1 = sampleFrame.bitmap(10);
        PqlBitmapQuery b2 = sampleFrame.bitmap(20);
        PqlBaseQuery q1 = sampleIndex.xor(b1, b2);
        assertEquals(
                "Xor(Bitmap(row=10, frame='sample-frame'), Bitmap(row=20, frame='sample-frame'))",
                q1.serialize());
    }

    @Test
    public void countTest() {
        PqlBitmapQuery b = collabFrame.bitmap(42);
        PqlQuery q = projectIndex.count(b);
        assertEquals(
                "Count(Bitmap(row=42, frame='collaboration'))",
                q.serialize());
    }

    @Test
    public void topNTest() {
        PqlQuery q1 = sampleFrame.topN(27);
        assertEquals(
                "TopN(frame='sample-frame', n=27, inverse=false)",
                q1.serialize());

        q1 = sampleFrame.inverseTopN(27);
        assertEquals(
                "TopN(frame='sample-frame', n=27, inverse=true)",
                q1.serialize());

        PqlQuery q2 = sampleFrame.topN(10, collabFrame.bitmap(3));
        assertEquals(
                "TopN(Bitmap(row=3, frame='collaboration'), frame='sample-frame', n=10, inverse=false)",
                q2.serialize());

        q2 = sampleFrame.inverseTopN(10, collabFrame.bitmap(3));
        assertEquals(
                "TopN(Bitmap(row=3, frame='collaboration'), frame='sample-frame', n=10, inverse=true)",
                q2.serialize());

        PqlBaseQuery q3 = sampleFrame.topN(12, collabFrame.bitmap(7), "category", 80, 81);
        assertEquals(
                "TopN(Bitmap(row=7, frame='collaboration'), frame='sample-frame', n=12, inverse=false, field='category', filters=[80,81])",
                q3.serialize());

        q3 = sampleFrame.inverseTopN(12, collabFrame.bitmap(7), "category", 80, 81);
        assertEquals(
                "TopN(Bitmap(row=7, frame='collaboration'), frame='sample-frame', n=12, inverse=true, field='category', filters=[80,81])",
                q3.serialize());

        PqlBaseQuery q4 = sampleFrame.topN(5, null);
        assertEquals(
                "TopN(frame='sample-frame', n=5, inverse=false)",
                q4.serialize());
    }

    @Test(expected = PilosaException.class)
    public void topNInvalidValuesTest() {
        sampleFrame.topN(5, sampleFrame.bitmap(2), "category", 80, new Object());

    }

    @Test
    public void rangeTest() {
        Calendar start = Calendar.getInstance();
        start.set(1970, Calendar.JANUARY, 1, 0, 0);
        Calendar end = Calendar.getInstance();
        end.set(2000, Calendar.FEBRUARY, 2, 3, 4);
        PqlBaseQuery q = collabFrame.range(10, start.getTime(), end.getTime());
        assertEquals(
                "Range(row=10, frame='collaboration', start='1970-01-01T00:00', end='2000-02-02T03:04')",
                q.serialize());
        q = sampleFrame.range("b7feb014-8ea7-49a8-9cd8-19709161ab63", start.getTime(), end.getTime());
        assertEquals(
                "Range(row='b7feb014-8ea7-49a8-9cd8-19709161ab63', frame='sample-frame', start='1970-01-01T00:00', end='2000-02-02T03:04')",
                q.serialize());

        q = collabFrame.inverseRange(10, start.getTime(), end.getTime());
        assertEquals(
                "Range(col=10, frame='collaboration', start='1970-01-01T00:00', end='2000-02-02T03:04')",
                q.serialize());

        q = collabFrame.inverseRange("b7feb014-8ea7-49a8-9cd8-19709161ab63", start.getTime(), end.getTime());
        assertEquals(
                "Range(col='b7feb014-8ea7-49a8-9cd8-19709161ab63', frame='collaboration', start='1970-01-01T00:00', end='2000-02-02T03:04')",
                q.serialize());
    }

    @Test
    public void setRowAttrsTest() {
        Map<String, Object> attrsMap = new TreeMap<>();
        attrsMap.put("quote", "\"Don't worry, be happy\"");
        attrsMap.put("active", true);
        PqlQuery q = collabFrame.setRowAttrs(5, attrsMap);
        assertEquals(
                "SetRowAttrs(row=5, frame='collaboration', active=true, quote=\"\\\"Don't worry, be happy\\\"\")",
                q.serialize());
        q = collabFrame.setRowAttrs("b7feb014-8ea7-49a8-9cd8-19709161ab63", attrsMap);
        assertEquals(
                "SetRowAttrs(row='b7feb014-8ea7-49a8-9cd8-19709161ab63', frame='collaboration', active=true, quote=\"\\\"Don't worry, be happy\\\"\")",
                q.serialize());
    }

    @Test(expected = PilosaException.class)
    public void setBitmapAttrsInvalidValuesTest() {
        Map<String, Object> attrsMap = new TreeMap<>();
        attrsMap.put("color", "blue");
        attrsMap.put("happy", new Object());
        collabFrame.setRowAttrs(5, attrsMap);
    }

    @Test
    public void setColumnAttrsTest() {
        Map<String, Object> attrsMap = new TreeMap<>();
        attrsMap.put("quote", "\"Don't worry, be happy\"");
        attrsMap.put("happy", true);
        PqlQuery q = projectIndex.setColumnAttrs(5, attrsMap);
        assertEquals(
                "SetColumnAttrs(col=5, happy=true, quote=\"\\\"Don't worry, be happy\\\"\")",
                q.serialize());
        q = projectIndex.setColumnAttrs("b7feb014-8ea7-49a8-9cd8-19709161ab63", attrsMap);
        assertEquals(
                "SetColumnAttrs(col='b7feb014-8ea7-49a8-9cd8-19709161ab63', happy=true, quote=\"\\\"Don't worry, be happy\\\"\")",
                q.serialize());
    }

    @Test(expected = PilosaException.class)
    public void setColumnAttrsInvalidValuesTest() {
        Map<String, Object> attrsMap = new TreeMap<>();
        attrsMap.put("color", "blue");
        attrsMap.put("happy", new Object());
        projectIndex.setColumnAttrs(5, attrsMap);
    }

    @Test
    public void fieldLessThanTest() {
        PqlQuery q = sampleFrame.field("foo").lessThan(10);
        assertEquals(
                "Range(frame='sample-frame', foo < 10)",
                q.serialize());
    }

    @Test
    public void fieldLessThanOrEqualTest() {
        PqlQuery q = sampleFrame.field("foo").lessThanOrEqual(10);
        assertEquals(
                "Range(frame='sample-frame', foo <= 10)",
                q.serialize());

    }

    @Test
    public void fieldGreaterThanTest() {
        PqlQuery q = sampleFrame.field("foo").greaterThan(10);
        assertEquals(
                "Range(frame='sample-frame', foo > 10)",
                q.serialize());

    }

    @Test
    public void fieldGreaterThanOrEqualTest() {
        PqlQuery q = sampleFrame.field("foo").greaterThanOrEqual(10);
        assertEquals(
                "Range(frame='sample-frame', foo >= 10)",
                q.serialize());

    }

    @Test
    public void fieldEqualsTest() {
        PqlQuery q = sampleFrame.field("foo").equals(10);
        assertEquals(
                "Range(frame='sample-frame', foo == 10)",
                q.serialize());
    }

    @Test
    public void fieldNotEqualsTest() {
        PqlQuery q = sampleFrame.field("foo").notEquals(10);
        assertEquals(
                "Range(frame='sample-frame', foo != 10)",
                q.serialize());
    }

    @Test
    public void fieldNotNullTest() {
        PqlQuery q = sampleFrame.field("foo").notNull();
        assertEquals(
                "Range(frame='sample-frame', foo != null)",
                q.serialize());
    }

    @Test
    public void fieldBetweenTest() {
        PqlQuery q = sampleFrame.field("foo").between(10, 20);
        assertEquals(
                "Range(frame='sample-frame', foo >< [10,20])",
                q.serialize());

    }

    @Test
    public void fieldSumTest() {
        PqlQuery q = sampleFrame.field("foo").sum(sampleFrame.bitmap(10));
        assertEquals(
                "Sum(Bitmap(row=10, frame='sample-frame'), frame='sample-frame', field='foo')",
                q.serialize());
        q = sampleFrame.field("foo").sum();
        assertEquals(
                "Sum(frame='sample-frame', field='foo')",
                q.serialize()
        );
    }

    @Test
    public void fieldSetValueTest() {
        PqlQuery q = sampleFrame.field("foo").setValue(10, 20);
        assertEquals(
                "SetFieldValue(frame='sample-frame', col=10, foo=20)",
                q.serialize());
    }

    @Test(expected = ValidationException.class)
    public void invalidFieldTest() {
        sampleFrame.field("??foo");
    }
}
