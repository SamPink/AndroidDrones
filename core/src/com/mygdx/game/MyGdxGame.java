package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;


public class MyGdxGame extends ApplicationAdapter {
	public PerspectiveCamera cam;
	public CameraInputController camController;
	public ModelBatch modelBatch;
	public AssetManager assets;
	public Array<ModelInstance> instances = new Array<ModelInstance>();
	public Environment environment;
	public boolean loading; // loading flag used to determine if asset is in memory

	public Array<ModelInstance> blocks = new Array<ModelInstance>();
	public Array<ModelInstance> invaders = new Array<ModelInstance>();
	public ModelInstance ship;
	public ModelInstance space;

	protected Stage stage;
	protected Label label;
	protected BitmapFont font;
	protected StringBuilder stringBuilder;

	private int visibleCount;

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
		//TODO needs updated
		camController = new CameraInputController(cam);
		Gdx.input.setInputProcessor(camController);

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
	}

	/**
	 * loads assets into memory
	 * spawning objects
	 */
	private void doneLoading() {
		ship = new ModelInstance(assets.get("ship.g3db", Model.class));
		ship.transform.setToRotation(Vector3.Y, 180).trn(0, 0, 6f);
		instances.add(ship);

		Model blockModel = assets.get("block.obj", Model.class);
		for (float x = -5f; x <= 5f; x += 2f) {
			ModelInstance block = new ModelInstance(blockModel);
			block.transform.setToTranslation(x, 0, 3f);
			instances.add(block);
			blocks.add(block);
		}

		Model invaderModel = assets.get("invader.obj", Model.class);
		for (float x = -5f; x <= 5f; x += 2f) {
			for (float z = -8f; z <= 0f; z += 2f) {
				ModelInstance invader = new ModelInstance(invaderModel);
				invader.transform.setToTranslation(x, 0, z);
				instances.add(invader);
				invaders.add(invader);
			}
		}

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
		label.setText(stringBuilder);
		stage.draw();
	}

	protected boolean isVisible(final Camera cam, final ModelInstance instance) {
		Vector3 position = new Vector3();
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
}
