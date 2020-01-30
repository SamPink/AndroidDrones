package com.mygdx.game;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;

public class GameObject extends ModelInstance {
    public Shape shape;

    public GameObject (Model model, String rootNode, boolean mergeTransform) {
        super(model, rootNode, mergeTransform);
    }

    public boolean isVisible(Camera cam) {
        return shape == null ? false : shape.isVisible(transform, cam);
    }

    /** @return -1 on no intersection, or when there is an intersection: the squared distance between the center of this
     * object and the point on the ray closest to this object when there is intersection. */
    public float intersects(Ray ray) {
        return shape == null ? -1f : shape.intersects(transform, ray);
    }
}
