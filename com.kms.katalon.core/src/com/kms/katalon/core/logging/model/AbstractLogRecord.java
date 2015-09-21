package com.kms.katalon.core.logging.model;

import java.util.ArrayList;
import java.util.List;

import com.kms.katalon.core.logging.model.TestStatus.TestStatusValue;

public abstract class AbstractLogRecord implements ILogRecord {
    protected String name;
    protected String id;
    protected String source;
    protected String message;
    protected String description;
    protected long startTime;
    protected long endTime;
    protected List<ILogRecord> childRecords;
    protected ILogRecord parentLogRecord;

    public AbstractLogRecord(String name) {
        setName(name);
        childRecords = new ArrayList<ILogRecord>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public TestStatus getStatus() {
        TestStatus testStatus = new TestStatus();
        testStatus.setStatusValue(TestStatusValue.PASSED);
        for (ILogRecord logRecord : getChildRecords()) {
            if (!(logRecord instanceof TestCaseLogRecord && ((TestCaseLogRecord) logRecord).isOptional())
                    && (logRecord.getStatus().getStatusValue() == TestStatusValue.ERROR || logRecord.getStatus()
                            .getStatusValue() == TestStatusValue.FAILED)) {
                if (logRecord.getStatus().getStatusValue() == TestStatusValue.ERROR) {
                    testStatus.setStatusValue(TestStatusValue.ERROR);
                } else if (logRecord.getStatus().getStatusValue() == TestStatusValue.FAILED) {
                    testStatus.setStatusValue(TestStatusValue.FAILED);
                }
                setMessage(logRecord.getMessage());
                break;
            }
        }
        return testStatus;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean hasChildRecords() {
        return childRecords != null && childRecords.size() > 0;
    }

    @Override
    public ILogRecord[] getChildRecords() {
        return childRecords.toArray(new ILogRecord[childRecords.size()]);
    }

    @Override
    public void addChildRecord(ILogRecord childRecord) {
        childRecords.add(childRecord);
        childRecord.setParentLogRecord(this);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public ILogRecord getParentLogRecord() {
        return parentLogRecord;
    }

    public void setParentLogRecord(ILogRecord parentLogRecord) {
        this.parentLogRecord = parentLogRecord;
    }
}
