package com.nightingale.sheepgameandengine;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnAreaTouchListener;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.shape.IAreaShape;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.BaseGameActivity;

import android.hardware.SensorManager;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;

public class MainActivity extends BaseGameActivity implements
		IOnSceneTouchListener, IOnAreaTouchListener {

	// CONSTANTS
	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;
	private static final FixtureDef FIXTURE_DEF = PhysicsFactory
			.createFixtureDef(1, 0.2f, 0.1f);

	// FIELDS
	private BitmapTextureAtlas bitmapTextureAtlas;
	private ITextureRegion sheepTextureRegion;

	private Scene scene;
	private PhysicsWorld physicsWorld;
	private MouseJoint mouseJoint;
	private Body surface;

	@Override
	public EngineOptions onCreateEngineOptions() {

		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		// create engine object
		EngineOptions engine = new EngineOptions(true,
				ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(
						CAMERA_WIDTH, CAMERA_HEIGHT), camera);

		return engine;
	}

	@Override
	public void onCreateResources(
			OnCreateResourcesCallback pOnCreateResourcesCallback)
			throws Exception {

		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		bitmapTextureAtlas = new BitmapTextureAtlas(getTextureManager(), 128, 128);
		this.sheepTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(bitmapTextureAtlas, this, "sheep128.png", 0, 0);
		bitmapTextureAtlas.load();
		pOnCreateResourcesCallback.onCreateResourcesFinished();
	}

	@Override
	public void onCreateScene(OnCreateSceneCallback pOnCreateSceneCallback)
			throws Exception {

		this.scene = new Scene();
		this.scene.setBackground(new Background(0, 0, 0));
		this.scene.setOnSceneTouchListener(this);
		this.scene.setOnAreaTouchListener(this);

		this.physicsWorld = new PhysicsWorld(new Vector2(0,
				SensorManager.GRAVITY_EARTH), false);
		this.surface = this.physicsWorld.createBody(new BodyDef());

		// create a box where the physics will be tested
		final VertexBufferObjectManager vertexBufferObjectManager = this
				.getVertexBufferObjectManager();
		final Rectangle bottomWall = new Rectangle(20, CAMERA_HEIGHT - 20,
				CAMERA_WIDTH - 20, 20, vertexBufferObjectManager);
		final Rectangle leftWall = new Rectangle(20, 20, 20,
				CAMERA_HEIGHT - 20, vertexBufferObjectManager);
		final Rectangle rightWall = new Rectangle(CAMERA_WIDTH - 20, 20,
				CAMERA_WIDTH - 20, CAMERA_HEIGHT - 20,
				vertexBufferObjectManager);
		final Rectangle topWall = new Rectangle(20, 20, CAMERA_WIDTH - 20, 20,
				vertexBufferObjectManager);
		final Rectangle fence = new Rectangle(CAMERA_WIDTH / 2,
				CAMERA_HEIGHT - 200, 20, CAMERA_HEIGHT - 20,
				vertexBufferObjectManager);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0,
				0.01f, 0.5f);
		PhysicsFactory.createBoxBody(this.physicsWorld, bottomWall,
				BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physicsWorld, topWall,
				BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physicsWorld, rightWall,
				BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physicsWorld, leftWall,
				BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physicsWorld, fence,
				BodyType.StaticBody, wallFixtureDef);

		this.scene.attachChild(topWall);
		this.scene.attachChild(rightWall);
		this.scene.attachChild(leftWall);
		this.scene.attachChild(bottomWall);
		this.scene.attachChild(fence);

		this.scene.registerUpdateHandler(this.physicsWorld);

		pOnCreateSceneCallback.onCreateSceneFinished(scene);

	}

	@Override
	public void onPopulateScene(Scene pScene,
			OnPopulateSceneCallback pOnPopulateSceneCallback) throws Exception {

		loadSheep();
		pOnPopulateSceneCallback.onPopulateSceneFinished();
	}

	@Override
	public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
			ITouchArea pTouchArea, float pTouchAreaLocalX,
			float pTouchAreaLocalY) {
		if (pSceneTouchEvent.isActionDown()) {
			final IAreaShape sheep = (IAreaShape) pTouchArea;
			/*
			 * If we have a active MouseJoint, we are just moving it around
			 * instead of creating a second one.
			 */
			if (this.mouseJoint == null) {
				// this.mEngine.vibrate(100);
				this.mouseJoint = this.createMouseJoint(sheep,
						pTouchAreaLocalX, pTouchAreaLocalY);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
		if (this.physicsWorld != null) {
			switch (pSceneTouchEvent.getAction()) {
			case TouchEvent.ACTION_DOWN:
				return true;
			case TouchEvent.ACTION_MOVE:
				if (this.mouseJoint != null) {
					final Vector2 vec = Vector2Pool
							.obtain(pSceneTouchEvent.getX()
									/ PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT,
									pSceneTouchEvent.getY()
											/ PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT);
					this.mouseJoint.setTarget(vec);
					Vector2Pool.recycle(vec);
				}
				return true;
			case TouchEvent.ACTION_UP:
				if (this.mouseJoint != null) {
					this.physicsWorld.destroyJoint(this.mouseJoint);
					this.mouseJoint = null;
				}
				return true;
			}
			return false;
		}
		return false;
	}

	// ========================
	// METHODS
	// ========================

	private MouseJoint createMouseJoint(IAreaShape face,
			float pTouchAreaLocalX, float pTouchAreaLocalY) {
		final Body body = (Body) face.getUserData();
		final MouseJointDef mouseJointDef = new MouseJointDef();

		final Vector2 localPoint = Vector2Pool.obtain(
				(pTouchAreaLocalX - face.getWidth() * 0.5f)
						/ PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT,
				(pTouchAreaLocalY - face.getHeight() * 0.5f)
						/ PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT);
		this.surface.setTransform(localPoint, 0);

		mouseJointDef.bodyA = this.surface;
		mouseJointDef.bodyB = body;
		mouseJointDef.dampingRatio = 0.95f;
		mouseJointDef.frequencyHz = 30;
		mouseJointDef.maxForce = (200.0f * body.getMass());
		mouseJointDef.collideConnected = true;

		mouseJointDef.target.set(body.getWorldPoint(localPoint));
		Vector2Pool.recycle(localPoint);

		return (MouseJoint) this.physicsWorld.createJoint(mouseJointDef);
	}

	private void loadSheep() {
		final Sprite sheep = new Sprite(CAMERA_WIDTH / 2, CAMERA_HEIGHT / 2,
				this.sheepTextureRegion, this.getVertexBufferObjectManager());
		
		final float width = sheep.getWidthScaled()/32;
		final float height = sheep.getWidthScaled()/32;
		final Vector2[] vertices = {  
				new Vector2(-0.41797f*width, -0.44531f*height),
				new Vector2(-0.19922f*width, -0.48828f*height),
				new Vector2(+0.06641f*width, -0.52344f*height),
				new Vector2(+0.39844f*width, -0.34375f*height),
				new Vector2(+0.51172f*width, -0.00391f*height),
				new Vector2(+0.35156f*width, +0.50000f*height),
				new Vector2(-0.28516f*width, +0.50000f*height),
				new Vector2(-0.43750f*width, +0.01172f*height)


		 };
		
		
		final Body sheepBody = PhysicsFactory.createPolygonBody(physicsWorld, sheep, vertices, BodyType.DynamicBody, FIXTURE_DEF);
		sheep.setUserData(sheepBody);
		// sheep.animate(200);
		//DebugRender debug = new DebugRender();

		this.scene.registerTouchArea(sheep);
		this.scene.attachChild(sheep);

		this.physicsWorld.registerPhysicsConnector(new PhysicsConnector(sheep,
				sheepBody, true, true));
		
		
		
	}
}