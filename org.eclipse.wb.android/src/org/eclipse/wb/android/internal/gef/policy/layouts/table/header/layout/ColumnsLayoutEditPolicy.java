package org.eclipse.wb.android.internal.gef.policy.layouts.table.header.layout;

import org.eclipse.wb.android.internal.gef.policy.layouts.table.TableLayoutEditPolicy;
import org.eclipse.wb.android.internal.gef.policy.layouts.table.header.part.ColumnHeaderEditPart;
import org.eclipse.wb.android.internal.gef.policy.layouts.table.header.selection.ColumnSelectionEditPolicy;
import org.eclipse.wb.android.internal.model.layouts.table.TableLayoutInfo;
import org.eclipse.wb.core.gef.figure.TextFeedback;
import org.eclipse.wb.core.gef.header.AbstractHeaderLayoutEditPolicy;
import org.eclipse.wb.core.gef.policy.layout.grid.AbstractGridLayoutEditPolicy;
import org.eclipse.wb.core.gef.policy.layout.grid.IGridInfo;
import org.eclipse.wb.draw2d.Figure;
import org.eclipse.wb.draw2d.FigureUtils;
import org.eclipse.wb.draw2d.Layer;
import org.eclipse.wb.draw2d.geometry.Interval;
import org.eclipse.wb.draw2d.geometry.Point;
import org.eclipse.wb.draw2d.geometry.Rectangle;
import org.eclipse.wb.gef.core.Command;
import org.eclipse.wb.gef.core.EditPart;
import org.eclipse.wb.gef.core.IEditPartViewer;
import org.eclipse.wb.gef.core.policies.EditPolicy;
import org.eclipse.wb.gef.core.requests.ChangeBoundsRequest;
import org.eclipse.wb.gef.core.requests.IDropRequest;
import org.eclipse.wb.gef.core.requests.Request;
import org.eclipse.wb.gef.graphical.policies.LayoutEditPolicy;

import java.text.MessageFormat;

/**
 * Implementation of {@link LayoutEditPolicy} for {@link ColumnHeaderEditPart}.
 * 
 * @author mitin_aa
 * @coverage android.gef.policy
 */
public final class ColumnsLayoutEditPolicy extends AbstractHeaderLayoutEditPolicy {
  private final TableLayoutInfo m_tableLayout;
  private final TableLayoutEditPolicy m_mainPolicy;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  public ColumnsLayoutEditPolicy(TableLayoutEditPolicy mainPolicy, TableLayoutInfo tableLayout) {
    super(mainPolicy);
    m_mainPolicy = mainPolicy;
    m_tableLayout = tableLayout;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Children
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  protected void decorateChild(EditPart child) {
    child.installEditPolicy(EditPolicy.SELECTION_ROLE, new ColumnSelectionEditPolicy(m_mainPolicy));
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Move
  //
  ////////////////////////////////////////////////////////////////////////////
  private final Figure m_insertFeedback = AbstractGridLayoutEditPolicy.createInsertFigure();
  private TextFeedback m_feedback;
  private Command m_moveCommand;

  @Override
  protected Command getMoveCommand(ChangeBoundsRequest request) {
    return m_moveCommand;
  }

  @Override
  protected void showLayoutTargetFeedback(Request request) {
    // prepare header
    ColumnHeaderEditPart headerEditPart;
    {
      ChangeBoundsRequest changeBoundsRequest = (ChangeBoundsRequest) request;
      headerEditPart = (ColumnHeaderEditPart) changeBoundsRequest.getEditParts().get(0);
    }
    // prepare location
    Point location;
    {
      IDropRequest dropRequest = (IDropRequest) request;
      location = dropRequest.getLocation().getCopy();
    }
    // prepare target header
    ColumnHeaderEditPart target = null;
    {
      for (EditPart editPart : getHost().getChildren()) {
        ColumnHeaderEditPart columnEditPart = (ColumnHeaderEditPart) editPart;
        Rectangle bounds = columnEditPart.getFigure().getBounds();
        if (location.x < bounds.getCenter().x) {
          target = columnEditPart;
          break;
        }
      }
    }
    // prepare grid information
    IGridInfo gridInfo = m_tableLayout.getGridInfo();
    Interval[] columnIntervals = gridInfo.getColumnIntervals();
    Interval[] rowIntervals = gridInfo.getRowIntervals();
    int y1 = rowIntervals[0].begin - 5;
    int y2 = rowIntervals[rowIntervals.length - 1].end() + 5;
    // prepare index of target column and position for insert feedbacks
    final int targetIndex;
    int x;
    int size = AbstractGridLayoutEditPolicy.INSERT_COLUMN_SIZE;
    if (target != null) {
      targetIndex = target.getHeader().getIndex();
      x = columnIntervals[targetIndex].begin - size / 2;
      if (targetIndex != 0) {
        x -= (columnIntervals[targetIndex].begin - columnIntervals[targetIndex - 1].end()) / 2;
      }
    } else {
      targetIndex = m_tableLayout.getLayoutSupport().getColumnCount();
      x = columnIntervals[columnIntervals.length - 1].end() - size / 2;
    }
    // show insert feedbacks
    {
      // ...on main viewer
      m_mainPolicy.showInsertFeedbacks(new Rectangle(x, y1, size, y2 - y1), null);
      // ...on header viewer
      {
        if (m_insertFeedback.getParent() == null) {
          addFeedback(m_insertFeedback);
        }
        // set bounds
        Point offset = headerEditPart.getOffset();
        Rectangle bounds = new Rectangle(x + offset.x, 0, size, getHostFigure().getSize().height);
        m_insertFeedback.setBounds(bounds);
      }
    }
    // show text feedback
    {
      Layer feedbackLayer = getMainLayer(IEditPartViewer.FEEDBACK_LAYER);
      // add feedback
      if (m_feedback == null) {
        m_feedback = new TextFeedback(feedbackLayer);
        m_feedback.add();
      }
      // set feedback bounds
      {
        Point feedbackLocation = new Point(location.x + 30, 10);
        FigureUtils.translateAbsoluteToFigure(feedbackLayer, feedbackLocation);
        m_feedback.setLocation(feedbackLocation);
      }
      // set text
      m_feedback.setText(MessageFormat.format("column: {0}", targetIndex));
    }
    // prepare command XXX
    /*{
    	final int sourceIndex = headerEditPart.getIndex();
    	m_moveCommand = new EditCommand(m_layout) {
    		@Override
    		protected void executeEdit() throws Exception {
    			m_layout.moveColumn(sourceIndex, targetIndex);
    		}
    	};
    }*/
  }

  @Override
  protected void eraseLayoutTargetFeedback(Request request) {
    m_mainPolicy.eraseInsertFeedbacks();
    FigureUtils.removeFigure(m_insertFeedback);
    if (m_feedback != null) {
      m_feedback.remove();
      m_feedback = null;
    }
  }
}
