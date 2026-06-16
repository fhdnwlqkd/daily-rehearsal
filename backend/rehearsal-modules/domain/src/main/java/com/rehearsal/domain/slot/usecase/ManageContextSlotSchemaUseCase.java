package com.rehearsal.domain.slot.usecase;

import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotCommand;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotOptionCommand;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotSchemaCommand;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotSchemaItemCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotOptionCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotSchemaCommand;
import com.rehearsal.domain.slot.usecase.command.DeleteContextSlotSchemaItemCommand;
import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotCommand;
import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotOptionCommand;
import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotSchemaCommand;
import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotSchemaItemCommand;
import java.util.List;

public interface ManageContextSlotSchemaUseCase {

  List<String> listActiveContextSlotSchemaKeys();

  ContextSlotSchema createSchema(CreateContextSlotSchemaCommand command);

  ContextSlotSchema updateSchema(UpdateContextSlotSchemaCommand command);

  List<ContextSlot> listSlots();

  ContextSlot createSlot(CreateContextSlotCommand command);

  ContextSlot updateSlot(UpdateContextSlotCommand command);

  ContextSlot createOption(CreateContextSlotOptionCommand command);

  ContextSlotOption updateOption(UpdateContextSlotOptionCommand command);

  ContextSlotSchema createSchemaItem(CreateContextSlotSchemaItemCommand command);

  ContextSlotSchema updateSchemaItem(UpdateContextSlotSchemaItemCommand command);

  void deleteSchema(DeleteContextSlotSchemaCommand command);

  void deleteSlot(DeleteContextSlotCommand command);

  void deleteOption(DeleteContextSlotOptionCommand command);

  void deleteSchemaItem(DeleteContextSlotSchemaItemCommand command);
}
