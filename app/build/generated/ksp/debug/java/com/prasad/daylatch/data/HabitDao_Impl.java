package com.prasad.daylatch.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HabitDao_Impl implements HabitDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Habit> __insertionAdapterOfHabit;

  private final AppConverters __appConverters = new AppConverters();

  private final EntityInsertionAdapter<HabitLog> __insertionAdapterOfHabitLog;

  private final SharedSQLiteStatement __preparedStmtOfArchive;

  public HabitDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHabit = new EntityInsertionAdapter<Habit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `habits` (`id`,`name`,`type`,`targetPerWindow`,`windowDays`,`createdAt`,`archived`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Habit entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        final String _tmp = __appConverters.fromHabitType(entity.getType());
        statement.bindString(3, _tmp);
        statement.bindLong(4, entity.getTargetPerWindow());
        statement.bindLong(5, entity.getWindowDays());
        statement.bindLong(6, entity.getCreatedAt());
        final int _tmp_1 = entity.getArchived() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
      }
    };
    this.__insertionAdapterOfHabitLog = new EntityInsertionAdapter<HabitLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `habit_logs` (`id`,`habitId`,`date`,`status`,`completedAtUtc`,`note`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HabitLog entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getHabitId());
        statement.bindString(3, entity.getDate());
        final String _tmp = __appConverters.fromHabitStatus(entity.getStatus());
        statement.bindString(4, _tmp);
        if (entity.getCompletedAtUtc() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getCompletedAtUtc());
        }
        if (entity.getNote() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getNote());
        }
      }
    };
    this.__preparedStmtOfArchive = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE habits SET archived = 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object saveHabit(final Habit habit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHabit.insert(habit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveLog(final HabitLog log, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHabitLog.insert(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object archive(final String habitId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfArchive.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, habitId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfArchive.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Habit>> observeActive() {
    final String _sql = "SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habits"}, new Callable<List<Habit>>() {
      @Override
      @NonNull
      public List<Habit> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTargetPerWindow = CursorUtil.getColumnIndexOrThrow(_cursor, "targetPerWindow");
          final int _cursorIndexOfWindowDays = CursorUtil.getColumnIndexOrThrow(_cursor, "windowDays");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final List<Habit> _result = new ArrayList<Habit>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Habit _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final HabitType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __appConverters.toHabitType(_tmp);
            final int _tmpTargetPerWindow;
            _tmpTargetPerWindow = _cursor.getInt(_cursorIndexOfTargetPerWindow);
            final int _tmpWindowDays;
            _tmpWindowDays = _cursor.getInt(_cursorIndexOfWindowDays);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpArchived;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_1 != 0;
            _item = new Habit(_tmpId,_tmpName,_tmpType,_tmpTargetPerWindow,_tmpWindowDays,_tmpCreatedAt,_tmpArchived);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<HabitLog>> observeForDate(final String date) {
    final String _sql = "SELECT * FROM habit_logs WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habit_logs"}, new Callable<List<HabitLog>>() {
      @Override
      @NonNull
      public List<HabitLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHabitId = CursorUtil.getColumnIndexOrThrow(_cursor, "habitId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCompletedAtUtc = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAtUtc");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<HabitLog> _result = new ArrayList<HabitLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitLog _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHabitId;
            _tmpHabitId = _cursor.getString(_cursorIndexOfHabitId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final HabitStatus _tmpStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __appConverters.toHabitStatus(_tmp);
            final Long _tmpCompletedAtUtc;
            if (_cursor.isNull(_cursorIndexOfCompletedAtUtc)) {
              _tmpCompletedAtUtc = null;
            } else {
              _tmpCompletedAtUtc = _cursor.getLong(_cursorIndexOfCompletedAtUtc);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new HabitLog(_tmpId,_tmpHabitId,_tmpDate,_tmpStatus,_tmpCompletedAtUtc,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object logsInRange(final String habitId, final String fromDate, final String toDate,
      final Continuation<? super List<HabitLog>> $completion) {
    final String _sql = "SELECT * FROM habit_logs WHERE habitId = ? AND date >= ? AND date <= ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, habitId);
    _argIndex = 2;
    _statement.bindString(_argIndex, fromDate);
    _argIndex = 3;
    _statement.bindString(_argIndex, toDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<HabitLog>>() {
      @Override
      @NonNull
      public List<HabitLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHabitId = CursorUtil.getColumnIndexOrThrow(_cursor, "habitId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCompletedAtUtc = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAtUtc");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<HabitLog> _result = new ArrayList<HabitLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitLog _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHabitId;
            _tmpHabitId = _cursor.getString(_cursorIndexOfHabitId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final HabitStatus _tmpStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __appConverters.toHabitStatus(_tmp);
            final Long _tmpCompletedAtUtc;
            if (_cursor.isNull(_cursorIndexOfCompletedAtUtc)) {
              _tmpCompletedAtUtc = null;
            } else {
              _tmpCompletedAtUtc = _cursor.getLong(_cursorIndexOfCompletedAtUtc);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new HabitLog(_tmpId,_tmpHabitId,_tmpDate,_tmpStatus,_tmpCompletedAtUtc,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HabitLog>> observeLogsInRange(final String fromDate, final String toDate) {
    final String _sql = "SELECT * FROM habit_logs WHERE date >= ? AND date <= ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, fromDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, toDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habit_logs"}, new Callable<List<HabitLog>>() {
      @Override
      @NonNull
      public List<HabitLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHabitId = CursorUtil.getColumnIndexOrThrow(_cursor, "habitId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCompletedAtUtc = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAtUtc");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<HabitLog> _result = new ArrayList<HabitLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitLog _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpHabitId;
            _tmpHabitId = _cursor.getString(_cursorIndexOfHabitId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final HabitStatus _tmpStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __appConverters.toHabitStatus(_tmp);
            final Long _tmpCompletedAtUtc;
            if (_cursor.isNull(_cursorIndexOfCompletedAtUtc)) {
              _tmpCompletedAtUtc = null;
            } else {
              _tmpCompletedAtUtc = _cursor.getLong(_cursorIndexOfCompletedAtUtc);
            }
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new HabitLog(_tmpId,_tmpHabitId,_tmpDate,_tmpStatus,_tmpCompletedAtUtc,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
