package com.example.arthreedmodelrotation

import android.opengl.Matrix
import android.util.Log
import org.the3deer.android_3d_model_engine.drawer.RendererFactory
import org.the3deer.android_3d_model_engine.event.SelectedObjectEvent
import org.the3deer.android_3d_model_engine.gui.CheckList
import org.the3deer.android_3d_model_engine.gui.GUI
import org.the3deer.android_3d_model_engine.gui.Glyph
import org.the3deer.android_3d_model_engine.gui.Menu3D
import org.the3deer.android_3d_model_engine.gui.Menu3D.ItemSelected
import org.the3deer.android_3d_model_engine.gui.Rotator
import org.the3deer.android_3d_model_engine.gui.Text
import org.the3deer.android_3d_model_engine.gui.Widget
import org.the3deer.android_3d_model_engine.model.Camera
import org.the3deer.android_3d_model_engine.objects.Axis
import org.the3deer.android_3d_model_engine.services.SceneLoader
import org.the3deer.android_3d_model_engine.view.FPSEvent
import org.the3deer.android_3d_model_engine.view.ModelSurfaceView
import org.the3deer.util.math.Quaternion
import java.util.EventObject
import java.util.Locale

internal class ModelViewerGUI(
    private val glView: ModelSurfaceView,
    private val scene: SceneLoader
) : GUI() {
    private var fps: Text? = null
    private var info: Text? = null
    private var axis: Widget? = null
    private var icon: Widget? = null
    private val icon2 = Glyph.build(Glyph.CHECKBOX_ON)
    private var menu: Menu3D? = null
    private var rotator: Rotator? = null

    init {
        color = floatArrayOf(1f, 1f, 1f, 0f)
        padding = PADDING_01
    }

    /**
     * @param width screenpixels
     * @param height screen y pixels
     */
    override fun setSize(width: Int, height: Int) {

        // log event
        Log.i("ModelViewerGUI", "New size: $width/$height")
        super.setSize(width, height)
        try {
            initFPS()
            initInfo()
            initAxis()
            //initMenu();
            //initMenu2();
        } catch (e: Exception) {
            Log.e("ModelViewerGUI", e.message, e)
            throw RuntimeException(e)
        }
    }

    private fun initFPS() {
        // frame-per-second
        if (fps != null) return
        fps = Text.allocate(7, 1)
        fps?.setId("fps")
        fps?.setVisible(true)
        fps?.setParent(this)
        fps?.setRelativeScale(floatArrayOf(0.15f, 0.15f, 0.15f))
        addWidget(fps)
        fps?.setPosition(POSITION_TOP_LEFT)
        //addBackground(fps).setColor(new float[]{0.25f, 0.25f, 0.25f, 0.25f});
    }

    private fun initInfo() {
        // model info
        if (info != null) return
        info = Text.allocate(15, 3, PADDING_01)
        info?.setId("info")
        info?.setVisible(true)
        info?.setParent(this)
        //info.setRelativeScale(new float[]{0.85f,0.85f,0.85f});
        info?.setRelativeScale(floatArrayOf(0.25f, 0.25f, 0.25f))
        addWidget(info)
        info?.setPosition(POSITION_BOTTOM)
        //addBackground(fps).setColor(new float[]{0.25f, 0.25f, 0.25f, 0.25f});
    }

    private fun initAxis() {
        if (axis != null) return
        axis = object : Widget(Axis.build()) {
            val matrix = FloatArray(16)
//            val orientation = Quaternion(matrix)
            override fun render(
                rendererFactory: RendererFactory,
                camera: Camera,
                lightPosInWorldSpace: FloatArray,
                colorMask: FloatArray
            ) {
                if (camera.hasChanged()) {
                    Matrix.setLookAtM(
                        matrix, 0, camera.getxPos(), camera.getyPos(), camera.getzPos(),
                        0f, 0f, 0f, camera.getxUp(), camera.getyUp(), camera.getzUp()
                    )
                    setOrientation(orientation)
                }
                super.render(rendererFactory, camera, lightPosInWorldSpace, colorMask)
            }
        }
        axis?.id = "gui_axis"
        axis?.isVisible = true
        axis?.parent = this
        axis?.setRelativeScale(floatArrayOf(0.1f, 0.1f, 0.1f))
        addWidget(axis)
        axis?.setPosition(POSITION_TOP_RIGHT)
    }

    private fun initMenu2() {
        // checklist
        val menuB = CheckList.Builder()
        menuB.add("lights")
        menuB.add("wireframe")
        menuB.add("textures")
        menuB.add("colors")
        menuB.add("animation")
        menuB.add("stereoscopic")
        menuB.add("------------")
        menuB.add("Load texture")
        menuB.add("------------")
        menuB.add("    Close   ")
        val menu2 = menuB.build()
        menu2.scale = floatArrayOf(0.1f, 0.1f, 0.1f)
        menu2.addListener(this)
        menu2.isVisible = true
        menu2.parent = icon
        menu2.setPosition(POSITION_BOTTOM)
        super.addWidget(menu2)
    }

    private fun initMenu() {
        // icon
        icon = Glyph.build(Glyph.MENU)
        icon?.parent = this
        icon?.setRelativeScale(floatArrayOf(0.1f, 0.1f, 0.1f))
        super.addWidget(icon)
        icon?.setPosition(POSITION_TOP_LEFT)
        icon?.isVisible = true
        //super.addBackground(icon).setColor(new float[]{0.25f, 0.25f, 0.25f, 0.25f});

        // menu
        val options: MutableList<String> = ArrayList()
        options.add("lights")
        options.add("wireframe")
        options.add("textures")
        options.add("colors")
        options.add("animation")
        options.add("stereoscopic")
        options.add("------------")
        options.add("Load texture")
        options.add("------------")
        options.add("    Close   ")
        menu = Menu3D.build(options.toTypedArray())
        menu?.addListener(this)
        menu?.isVisible = icon?.isVisible?.not() ?: false
        menu?.parent = this
        menu?.setRelativeScale(floatArrayOf(0.5f, 0.5f, 0.5f))
        //menu.setColor(new float[]{0.25f, 0.25f, 0.25f, 0.25f});
        //icon.addListener(menu);
        super.addWidget(menu)
        menu?.setPosition(POSITION_MIDDLE)
        super.addBackground(menu).color = floatArrayOf(0.5f, 0f, 0f, 0.25f)

        // menu rotator
        rotator = Rotator.build(menu)
        rotator?.color = floatArrayOf(1f, 0f, 0f, 1f)
        menu?.isVisible?.let { rotator?.setVisible(it) }
        rotator?.location = menu?.location
        rotator?.scale = menu?.scale
        super.addWidget(rotator)
        //super.addBackground(rotator).setColor(new float[]{0.5f, 0.5f, 0f, 0.25f});
    }

    override fun onEvent(event: EventObject): Boolean {
        super.onEvent(event)
        if (event is FPSEvent) {
            if (fps!!.isVisible) {
                fps!!.update(event.fps.toString() + " fps")
            }
        } else if (event is SelectedObjectEvent) {
            if (info!!.isVisible) {
                val selected = event.selected
                val info = StringBuilder()
                if (selected != null) {
                    if (selected.id.indexOf('/') == -1) {
                        info.append(selected.id)
                    } else {
                        info.append(selected.id.substring(selected.id.lastIndexOf('/') + 1))
                    }
                    info.append('\n')
                    info.append("size: ")
                    info.append(
                        String.format(
                            Locale.getDefault(),
                            "%.2f",
                            selected.dimensions.largest
                        )
                    )
                    info.append('\n')
                    info.append("scale: ")
                    info.append(String.format(Locale.getDefault(), "%.2f", selected.scaleX))
                    //final DecimalFormat df = new DecimalFormat("0.##");
                    //info.append(df.format(selected.getScaleX()));
                    info.append("x")
                }
                Log.v("ModelViewerGUI", "Selected object info: $info")
                this.info!!.update(info.toString().lowercase(Locale.getDefault()))
            }
        } else if (event is ItemSelected) {
            when (event.selected) {
                0 -> {
                    Log.i("ModelViewerGUI", "Toggling lights...")
                    glView.toggleLights()
                    menu!!.setState(0, glView.isLightsEnabled)
                }

                1 -> glView.toggleWireframe()
                2 -> glView.toggleTextures()
                3 -> glView.toggleColors()
                4 -> glView.toggleAnimation()
                9 -> menu!!.isVisible = false
            }
        } else if (event is ClickEvent) {
            val widget = event.widget
            Log.i("ModelViewerGUI", "Click... widget: " + widget.id)
            if (widget === icon) {
                menu!!.toggleVisible()
            }
            if (widget === icon2) {
                if (icon2.code == Glyph.CHECKBOX_ON) {
                    icon2.code = Glyph.CHECKBOX_OFF
                } else {
                    icon2.code = Glyph.CHECKBOX_ON
                }
            }
            if (widget === menu) {
                //menu.onEvent(event);
            }
            if (widget === rotator) {
                //rotator.onEvent(clickEvent);
            }
        } else if (event is MoveEvent) {
            val dx = event.dx
            val dy = event.dy
            val newPosition = event.widget.location.clone()
            newPosition[1] += dy
            event.widget.location = newPosition
        }
        return true
    }
}