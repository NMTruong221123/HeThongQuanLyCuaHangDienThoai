import os
root='e:/PhoneStoreManagement/src'
count=0
for dirpath,dirs,files in os.walk(root):
    for f in files:
        if f.endswith('.java'):
            p=os.path.join(dirpath,f)
            with open(p,'rb') as fh:
                data=fh.read()
            if data.startswith(b'\xef\xbb\xbf'):
                with open(p,'wb') as fh:
                    fh.write(data[3:])
                print('stripped BOM:',p)
                count+=1
print('done',count,'files')
