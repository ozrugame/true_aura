package dev.trueaura.client;

import dev.trueaura.aura.CustomAura;

/** Bounded tessellation that preserves every imported profile breakpoint. */
public final class CustomAuraMesh {
    public static void emit(double time,float playerHeight,CustomAura definition,boolean distant,AuraMesh.VertexSink sink) {
        int sides=distant?16:32, rows=distant?8:16;
        for(var layer:definition.layers()) {
            var grid=new java.util.TreeSet<Float>();
            for(int row=0;row<=rows;row++) grid.add((float)row/rows);
            for(var point:layer.profile()) grid.add((float)point.y());
            var levels=new java.util.ArrayList<>(grid);
            for(int side=0;side<sides;side++) for(int row=0;row<levels.size()-1;row++) {
                float u0=(float)side/sides,u1=(side+1f)/sides,v0=levels.get(row),v1=levels.get(row+1);
                point(time,playerHeight,layer,u0,v0,sink); point(time,playerHeight,layer,u1,v0,sink);
                point(time,playerHeight,layer,u1,v1,sink); point(time,playerHeight,layer,u0,v1,sink);
            }
        }
    }
    private static void point(double time,float height,CustomAura.Layer layer,float u,float v,AuraMesh.VertexSink sink) {
        var points=layer.profile(); double radius=points.get(points.size()-1).radius();
        for(int i=1;i<points.size();i++) if(v<=points.get(i).y()) {
            var a=points.get(i-1); var b=points.get(i); double t=(v-a.y())/(b.y()-a.y());
            radius=a.radius()+(b.radius()-a.radius())*t; break;
        }
        double angle=u*2*Math.PI;
        radius=Math.max(0,radius+layer.amplitude()*Math.sin(angle*layer.waves()+v*8-time*layer.speed())*Math.sin(v*Math.PI));
        angle+=v*layer.twist()*2*Math.PI;
        sink.vertex((float)(Math.cos(angle)*radius),(float)(layer.offset()+v*height*layer.height()),
            (float)(Math.sin(angle)*radius),u,v,layer.role());
    }
    private CustomAuraMesh() {}
}
