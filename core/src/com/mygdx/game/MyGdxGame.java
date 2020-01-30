package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;


public class MyGdxGame extends InputAdapter implements ApplicationListener  {
	public PerspectiveCamera cam;
	public CameraInputController camController;
	public ModelBatch modelBatch;
	public AssetManager assets;
	public Array<GameObject> instances = new Array<GameObject>();
	public Environment environment;
	public boolean loading; // loading flag used to determine if asset is in memory

	public Array<GameObject> blocks = new Array<>();
	public ModelInstance block;
	public ModelInstance space;

	protected Stage stage;
	protected Label label;
	protected BitmapFont font;
	protected StringBuilder stringBuilder;

	private int visibleCount;
    private Vector3 position = new Vector3();

    //store which ModelInstance within the instances array is selected or currently being selected
	private int selected = -1, selecting = -1;

	//used to store which object is being interacted with
    private Material selectionMaterial;
    private Material originalMaterial;


	@Override
	public void create () {

		modelBatch = new ModelBatch(); //used to store game models
		environment = new Environment(); //stores the game world
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f)); //sets lighting conditions
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		//creates game camera and view position
		cam = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0f, 7f, 10f);
		cam.lookAt(0,0,0);
		cam.near = 1f;
		cam.far = 300f;
		cam.update();

		//implements default controller input
		camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(new InputMultiplexer(this, camController));

		//class all assets are stored within
		assets = new AssetManager();
		assets.load("ship.g3db", Model.class);
		assets.load("block.obj", Model.class);
		assets.load("invader.obj", Model.class);
		assets.load("spacesphere.obj", Model.class);
		loading = true;

		stage = new Stage(); //HUD element
		label = new Label(" ", new Label.LabelStyle(new BitmapFont(), Color.WHITE));//create empty label
		stage.addActor(label); //add label to screen
		stringBuilder = new StringBuilder();

        selectionMaterial = new Material();
        selectionMaterial.set(ColorAttribute.createDiffuse(Color.ORANGE));
        originalMaterial = new Material();

	}

	/**
	 * loads assets into memory
	 * spawning objects
	 */
	private void doneLoading() {
        Model model = assets.get("ship.g3db", Model.class);
        GameObject ship = new GameObject(model, model.nodes.get(0).id, true);
		ship.transform.setToRotation(Vector3.Y, 180).trn(0, 0, 6f);
		instances.add(ship);


		space = new ModelInstance(assets.get("spacesphere.obj", Model.class));

		loading = false; //loading is false when objects are in memory
	}

	@Override
	public void render () {
		if (loading && assets.update()) //true if objects are not in memory or is objects have changed
			doneLoading();//load into memory
		camController.update();//update the view

		//clear canvas
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(cam);
		visibleCount = 0;
		for (final ModelInstance instance : instances) {//all objects in game
			if (isVisible(cam, instance)) {
				modelBatch.render(instance, environment);//object is viable in current view
				visibleCount++;
			}
		}
		if (space != null)
			modelBatch.render(space);
		modelBatch.end();

		//write components to HUD
		stringBuilder.setLength(0);
		stringBuilder.append(" FPS: ").append(Gdx.graphics.getFramesPerSecond());
		stringBuilder.append(" Visible: ").append(visibleCount);
        stringBuilder.append(" Selected: ").append(selected);
		label.setText(stringBuilder);
		stage.draw();
	}

	protected boolean isVisible(final Camera cam, final ModelInstance instance) {
		instance.transform.getTranslation(position);
		return cam.frustum.pointInFrustum(position);
	}

	@Override
	public void dispose () {
		modelBatch.dispose();
		instances.clear();
		assets.dispose();
	}

	@Override
	public void resume () {
	}

	@Override
	public void resize (int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void pause () {
	}

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        selecting = getObject(screenX, screenY);
        return selecting >= 0;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return selecting >= 0;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (selecting >= 0) {
            if (selecting == getObject(screenX, screenY))
                setSelected(selecting);
            selecting = -1;
            return true;
        }
        setSelected(-1);
        return false;
    }

    public void setSelected (int value) {
        if (selected == value) return;
        if (selected >= 0) {
            Material mat = instances.get(selected).materials.get(0);
            mat.clear();
            mat.set(originalMaterial);
        }
        selected = value;
        if (selected >= 0) {
            Material mat = instances.get(selected).materials.get(0);
            originalMaterial.clear();
            originalMaterial.set(mat);
            mat.clear();
            mat.set(selectionMaterial);
        }
    }

    public int getObject (int screenX, int screenY) {
        Ray ray = cam.getPickRay(screenX, screenY);

        int result = -1;
        float distance = -1;

        for (int i = 0; i < instances.size; ++i) {
            final GameObject instance =  instances.get(i);

            instance.transform.getTranslation(position);
            position.add(instance.center);

            final float len = ray.direction.dot(position.x-ray.origin.x, position.y-ray.origin.y, position.z-ray.origin.z);
            if (len < 0f)
                continue;

            float dist2 = position.dst2(ray.origin.x+ray.direction.x*len, ray.origin.y+ray.direction.y*len, ray.origin.z+ray.direction.z*len);
            if (distance >= 0f && dist2 > distance)
                continue;

            if (dist2 <= instance.radius * instance.radius) {
                result = i;
                distance = dist2;
            }
        }
        return result;
    }
}
