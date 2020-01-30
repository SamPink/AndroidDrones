package com.mygdx.game;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;

public class Disk extends BaseShape {
    public float radius;
    public Disk(BoundingBox bounds) {
        super(bounds);
        radius = 0.5f * (dimensions.x > dimensions.z ? dimensions.x : dimensions.z);
    }

    @Override
    public boolean isVisible (Matrix4 transform, Camera cam) {
        return cam.frustum.sphereInFrustum(transform.getTranslation(position).add(center), radius);
    }

    @Override
    public float intersects (Matrix4 transform, Ray ray) {
        transform.getTranslation(position).add(center);
        final float len = (position.y - ray.origin.y) / ray.direction.y;
        final float dist2 = position.dst2(ray.origin.x + len * ray.direction.x, ray.origin.y + len * ray.direction.y, ray.origin.z + len * ray.direction.z);
        return (dist2 < radius * radius) ? dist2 : -1f;
    }
}
