package com.mygdx.game;

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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
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

    protected Array<GameObject> blocks = new Array<GameObject>();
    protected Array<GameObject> invaders = new Array<GameObject>();
    protected ModelInstance ship;
    protected ModelInstance space;

    protected Shape blockShape;
    protected Shape invaderShape;
    protected Shape shipShape;

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
		assets.load("invaderscene.g3db",Model.class);
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
    private BoundingBox bounds = new BoundingBox();
    private void doneLoading () {
        Model model = assets.get("invaderscene.g3db", Model.class);
        for (int i = 0; i < model.nodes.size; i++) {
            String id = model.nodes.get(i).id;
            GameObject instance = new GameObject(model, id, true);

            if (id.equals("space")) {
                space = instance;
                continue;
            }

            instances.add(instance);

            if (id.equals("ship")) {
                instance.calculateBoundingBox(bounds);
                shipShape = new Sphere(bounds);
                instance.shape = shipShape;
                ship = instance;
            }
            else if (id.startsWith("block")) {
                if (blockShape == null) {
                    instance.calculateBoundingBox(bounds);
                    blockShape = new Box(bounds);
                }
                instance.shape = blockShape;
                blocks.add(instance);
            }
            else if (id.startsWith("invader")) {
                if (invaderShape == null) {
                    instance.calculateBoundingBox(bounds);
                    invaderShape = new Disk(bounds);
                }
                instance.shape = invaderShape;
                invaders.add(instance);
            }
        }

        loading = false;
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
            final GameObject instance = instances.get(i);
            float dist2 = instance.intersects(ray);
            if (dist2 >= 0 && (distance < 0f || dist2 < distance)) {
                result = i;
                distance = dist2;
            }
        }
        return result;
    }
}
