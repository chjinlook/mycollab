/**
 * This file is part of mycollab-mobile.
 *
 * mycollab-mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mycollab-mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mycollab-mobile.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.esofthead.mycollab.mobile.module.crm.view.activity;

import com.esofthead.mycollab.common.i18n.GenericI18Enum;
import com.esofthead.mycollab.core.MyCollabException;
import com.esofthead.mycollab.core.arguments.NumberSearchField;
import com.esofthead.mycollab.eventmanager.EventBus;
import com.esofthead.mycollab.mobile.module.crm.events.ActivityEvent;
import com.esofthead.mycollab.mobile.module.crm.ui.CrmGenericPresenter;
import com.esofthead.mycollab.mobile.ui.ConfirmDialog;
import com.esofthead.mycollab.module.crm.CrmLinkGenerator;
import com.esofthead.mycollab.module.crm.domain.SimpleTask;
import com.esofthead.mycollab.module.crm.domain.Task;
import com.esofthead.mycollab.module.crm.domain.criteria.TodoSearchCriteria;
import com.esofthead.mycollab.module.crm.service.TaskService;
import com.esofthead.mycollab.security.RolePermissionCollections;
import com.esofthead.mycollab.spring.ApplicationContextUtil;
import com.esofthead.mycollab.vaadin.AppContext;
import com.esofthead.mycollab.vaadin.events.DefaultPreviewFormHandler;
import com.esofthead.mycollab.vaadin.mvp.ScreenData;
import com.esofthead.mycollab.vaadin.ui.NotificationUtil;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.UI;

/**
 * 
 * @author MyCollab Ltd.
 * @since 4.1
 * 
 */
public class AssignmentReadPresenter extends
		CrmGenericPresenter<AssignmentReadView> {
	private static final long serialVersionUID = -5145707856679650546L;

	public AssignmentReadPresenter() {
		super(AssignmentReadView.class);
	}

	@Override
	protected void postInitView() {
		view.getPreviewFormHandlers().addFormHandler(
				new DefaultPreviewFormHandler<SimpleTask>() {
					@Override
					public void onEdit(SimpleTask data) {
						EventBus.getInstance().fireEvent(
								new ActivityEvent.TaskEdit(this, data));
					}

					@Override
					public void onAdd(SimpleTask data) {
						EventBus.getInstance().fireEvent(
								new ActivityEvent.TaskAdd(this, null));
					}

					@Override
					public void onDelete(final SimpleTask data) {
						ConfirmDialog.show(
								UI.getCurrent(),
								AppContext
										.getMessage(GenericI18Enum.DIALOG_CONFIRM_DELETE_RECORD_MESSAGE),
								AppContext
										.getMessage(GenericI18Enum.BUTTON_YES_LABEL),
								AppContext
										.getMessage(GenericI18Enum.BUTTON_NO_LABEL),
								new ConfirmDialog.CloseListener() {
									private static final long serialVersionUID = 1L;

									@Override
									public void onClose(ConfirmDialog dialog) {
										if (dialog.isConfirmed()) {
											TaskService taskService = ApplicationContextUtil
													.getSpringBean(TaskService.class);
											taskService.removeWithSession(
													data.getId(),
													AppContext.getUsername(),
													AppContext.getAccountId());
											EventBus.getInstance().fireEvent(
													new ActivityEvent.GotoList(
															this, null));
										}
									}
								});
					}

					@Override
					public void onClone(SimpleTask data) {
						Task cloneData = (Task) data.copy();
						cloneData.setId(null);
						EventBus.getInstance().fireEvent(
								new ActivityEvent.TaskEdit(this, cloneData));
					}

					@Override
					public void onCancel() {
						EventBus.getInstance().fireEvent(
								new ActivityEvent.GotoList(this, null));
					}

					@Override
					public void gotoNext(SimpleTask data) {
						TaskService taskService = ApplicationContextUtil
								.getSpringBean(TaskService.class);
						TodoSearchCriteria criteria = new TodoSearchCriteria();
						criteria.setSaccountid(new NumberSearchField(AppContext
								.getAccountId()));
						criteria.setId(new NumberSearchField(data.getId(),
								NumberSearchField.GREATER));
						Integer nextId = taskService.getNextItemKey(criteria);
						if (nextId != null) {
							EventBus.getInstance().fireEvent(
									new ActivityEvent.TaskRead(this, nextId));
						} else {
							NotificationUtil.showGotoLastRecordNotification();
						}

					}

					@Override
					public void gotoPrevious(SimpleTask data) {
						TaskService taskService = ApplicationContextUtil
								.getSpringBean(TaskService.class);
						TodoSearchCriteria criteria = new TodoSearchCriteria();
						criteria.setSaccountid(new NumberSearchField(AppContext
								.getAccountId()));
						criteria.setId(new NumberSearchField(data.getId(),
								NumberSearchField.LESSTHAN));
						Integer nextId = taskService
								.getPreviousItemKey(criteria);
						if (nextId != null) {
							EventBus.getInstance().fireEvent(
									new ActivityEvent.TaskRead(this, nextId));
						} else {
							NotificationUtil.showGotoFirstRecordNotification();
						}
					}
				});
	}

	@Override
	protected void onGo(ComponentContainer container, ScreenData<?> data) {
		if (AppContext.canRead(RolePermissionCollections.CRM_TASK)) {

			SimpleTask task = null;
			if (data.getParams() instanceof Integer) {
				TaskService taskService = ApplicationContextUtil
						.getSpringBean(TaskService.class);
				task = taskService.findById((Integer) data.getParams(),
						AppContext.getAccountId());
				if (task == null) {
					NotificationUtil.showRecordNotExistNotification();
					return;
				}
			} else {
				throw new MyCollabException("Invalid data " + data);
			}

			super.onGo(container, data);
			view.previewItem(task);
			AppContext.addFragment(CrmLinkGenerator
					.generateTaskPreviewLink(task.getId()), AppContext
					.getMessage(GenericI18Enum.BROWSER_PREVIEW_ITEM_TITLE,
							"Task", task.getSubject()));

		} else {
			NotificationUtil.showMessagePermissionAlert();
		}
	}

}
